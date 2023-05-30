
package com.alibaba.arthas.tunnel.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.arthas.tunnel.server.utils.InetAddressUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.alibaba.arthas.tunnel.common.URIConstans;
import com.alibaba.arthas.tunnel.server.utils.HttpUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 *
 * @author hengyunabc 2019-08-27
 *
 */
public class TunnelSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static List<Integer> DEFAULT_AGENT_PORTS = new LinkedList<>();
    private static int MAX_AGENT_PORT = 12300;
    private static int MIN_AGENT_PORT = 12295;
    private static String STATUS_SUCCESS = "SUCCESS@arthas";
    private static final String IPV4_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static final Pattern pattern = Pattern.compile(IPV4_PATTERN);
    private final static Logger logger = LoggerFactory.getLogger(TunnelSocketFrameHandler.class);

    static {
        for(int port = MAX_AGENT_PORT;port >= MIN_AGENT_PORT;port--){
            DEFAULT_AGENT_PORTS.add(port);
        }
    }
    private TunnelServer tunnelServer;

    public TunnelSocketFrameHandler(TunnelServer tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HandshakeComplete) {
            HandshakeComplete handshake = (HandshakeComplete) evt;
            // http request uri
            String uri = handshake.requestUri();
            logger.info("websocket handshake complete, uri: {}", uri);

            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(uri).build().getQueryParams();
            String method = parameters.getFirst(URIConstans.METHOD);

            if (MethodConstants.CONNECT_ARTHAS.equals(method)) { // form browser
                connectArthas(ctx, parameters);
            } else if (MethodConstants.AGENT_REGISTER.equals(method)) { // form arthas agent, register
                agentRegister(ctx, handshake, uri);
            }
            if (MethodConstants.OPEN_TUNNEL.equals(method)) { // from arthas agent open tunnel
                String clientConnectionId = parameters.getFirst(URIConstans.CLIENT_CONNECTION_ID);
                openTunnel(ctx, clientConnectionId);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 只有 arthas agent register建立的 channel 才可能有数据到这里
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String text = textFrame.text();

            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(text).build()
                    .getQueryParams();

            String method = parameters.getFirst(URIConstans.METHOD);

            /**
             * <pre>
             * 1. 之前http proxy请求已发送到 tunnel cleint，这里接收到 tunnel client的结果，并解析出SimpleHttpResponse
             * 2. 需要据 URIConstans.PROXY_REQUEST_ID 取出当时的 Promise，再设置SimpleHttpResponse进去
             * </pre>
             */
            if (MethodConstants.HTTP_PROXY.equals(method)) {
                final String requestIdRaw = parameters.getFirst(URIConstans.PROXY_REQUEST_ID);
                final String requestId;
                if (requestIdRaw != null) {
                    requestId = URLDecoder.decode(requestIdRaw, "utf-8");
                } else {
                    requestId = null;
                }
                if (requestId == null) {
                    logger.error("error, need {}, text: {}", URIConstans.PROXY_REQUEST_ID, text);
                    return;
                }
                logger.info("received http proxy response, requestId: {}", requestId);

                Promise<SimpleHttpResponse> promise = tunnelServer.findProxyRequestPromise(requestId);

                final String dataRaw = parameters.getFirst(URIConstans.PROXY_RESPONSE_DATA);
                final String data;
                if (dataRaw != null) {
                    data = URLDecoder.decode(dataRaw, "utf-8");
                    byte[] bytes = Base64.decodeBase64(data);

                    SimpleHttpResponse simpleHttpResponse = SimpleHttpResponse.fromBytes(bytes);
                    promise.setSuccess(simpleHttpResponse);
                } else {
                    data = null;
                    promise.setFailure(new Exception(URIConstans.PROXY_RESPONSE_DATA + " is null! reuqestId: " + requestId));
                }
            }
        }
    }

    private void connectArthas(ChannelHandlerContext tunnelSocketCtx, MultiValueMap<String, String> parameters) {
        List<String> agentId = parameters.getOrDefault("id", Collections.emptyList());
        if (agentId.isEmpty()) {
            logger.error("arthas agent id can not be null, parameters: {}", parameters);
            throw new IllegalArgumentException("arthas agent id can not be null");
        }

        String id = agentId.get(0);
        logger.info("try to connect to arthas agent, id: " + id);

        Optional<AgentInfo> findAgent = tunnelServer.findAgent(id);
        if(!findAgent.isPresent()) {
            String tunnelServerIp = InetAddressUtil.getInetAddress();
            int tunnelServerPort = this.tunnelServer.getPort();
            String ts = "ws://" + tunnelServerIp + ":" + tunnelServerPort + "/ws";

            String[] agentAddressAndPort = id.split(":");
            String agentAddress = agentAddressAndPort[0];
            if(!validate(agentAddress)){
                logger.error("arthas agent adress ip {} is invalid!", agentAddress);
                throw new IllegalArgumentException("arthas agent adress ip is invalid!");
            }

            List<Integer> ports = new LinkedList<>(DEFAULT_AGENT_PORTS);
            if(agentAddressAndPort.length > 1){
                try{
                    ports.add(0,Integer.valueOf(agentAddressAndPort[1]));
                }catch (NumberFormatException e){
                    logger.error("arthas agent port {} is invalid!", agentAddressAndPort[1]);
                    throw new IllegalArgumentException("arthas agent port is invalid!");
                }
            }

            for (int port : ports) {
                StringBuffer uri = new StringBuffer();
                uri.append("http://").append(agentAddress + ":" + port)
                        .append("/rDm?id=").append(id)
                        .append("&ts=").append(ts);
                if(tunnelServer.isEnableStatUrl()){
                    uri.append("&su=http://").append(tunnelServerIp).append(":").append(tunnelServer.getHttpPort())
                            .append("/openapi/stat");
                }
                if(tunnelServer.isEnableHttpPort()){
                    uri.append("&ehp=t");
                }
                logger.info("attach request url : {}", uri);
                try {
                    URL url = new URL(uri.toString());
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    int status = con.getResponseCode();
                    if (status != 200) {
                        findAgent = tunnelServer.findAgent(id);
                        break;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    if(!STATUS_SUCCESS.equals(content.toString()) ||
                            !"SUCCESS".equals(content.toString())){
                        findAgent = tunnelServer.findAgent(id);
                        break;
                    }
                    int count = 0;
                    while (true) {
                        Thread.sleep(1L);
                        findAgent = tunnelServer.findAgent(id);
                        if (findAgent.isPresent() || ++count > 2000) {
                            break;
                        }
                    }
                    break;
                } catch (Exception e) {
                    logger.error("dynamic attach agent error,attach request url {} , error message {}!", uri, e.getMessage());
                }
            }
        }

        if(!findAgent.isPresent()){
            tunnelSocketCtx.channel().writeAndFlush(new CloseWebSocketFrame(2000, "Can not find arthas agent by id: "+ agentId));
            logger.error("Can not find arthas agent by id: {}", agentId);
            throw new IllegalArgumentException("Can not find arthas agent by id: " + agentId);
        }

        ChannelHandlerContext agentCtx = findAgent.get().getChannelHandlerContext();

        String clientConnectionId = RandomStringUtils.random(20, true, true).toUpperCase();

        logger.info("random clientConnectionId: " + clientConnectionId);
        // URI uri = new URI("response", null, "/",
        //        "method=" + MethodConstants.START_TUNNEL + "&id=" + agentId.get(0) + "&clientConnectionId=" + clientConnectionId, null);
        URI uri = UriComponentsBuilder.newInstance().scheme(URIConstans.RESPONSE).path("/")
                .queryParam(URIConstans.METHOD, MethodConstants.START_TUNNEL).queryParam(URIConstans.ID, agentId)
                .queryParam(URIConstans.CLIENT_CONNECTION_ID, clientConnectionId).build().toUri();

        logger.info("startTunnel response: " + uri);

        ClientConnectionInfo clientConnectionInfo = new ClientConnectionInfo();
        SocketAddress remoteAddress = tunnelSocketCtx.channel().remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
            clientConnectionInfo.setHost(inetSocketAddress.getHostString());
            clientConnectionInfo.setPort(inetSocketAddress.getPort());
        }
        clientConnectionInfo.setChannelHandlerContext(tunnelSocketCtx);

        // when the agent open tunnel success, will set result into the promise
        Promise<Channel> promise = GlobalEventExecutor.INSTANCE.newPromise();
        promise.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(final Future<Channel> future) throws Exception {
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    tunnelSocketCtx.pipeline().remove(TunnelSocketFrameHandler.this);

                    // outboundChannel is form arthas agent
                    outboundChannel.pipeline().removeLast();

                    outboundChannel.pipeline().addLast(new RelayHandler(tunnelSocketCtx.channel()));
                    tunnelSocketCtx.pipeline().addLast(new RelayHandler(outboundChannel));
                } else {
                    logger.error("wait for agent connect error. agentId: {}, clientConnectionId: {}", agentId,
                            clientConnectionId);
                    ChannelUtils.closeOnFlush(agentCtx.channel());
                }
            }
        });

        clientConnectionInfo.setPromise(promise);
        this.tunnelServer.addClientConnectionInfo(clientConnectionId, clientConnectionInfo);
        tunnelSocketCtx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                tunnelServer.removeClientConnectionInfo(clientConnectionId);
            }
        });

        agentCtx.channel().writeAndFlush(new TextWebSocketFrame(uri.toString()));

        logger.info("browser connect waitting for arthas agent open tunnel");
        boolean watiResult = promise.awaitUninterruptibly(20, TimeUnit.SECONDS);
        if (watiResult) {
            logger.info(
                    "browser connect wait for arthas agent open tunnel success, agentId: {}, clientConnectionId: {}",
                    agentId, clientConnectionId);
        } else {
            logger.error(
                    "browser connect wait for arthas agent open tunnel timeout, agentId: {}, clientConnectionId: {}",
                    agentId, clientConnectionId);
            tunnelSocketCtx.close();
        }

    }

    private void agentRegister(ChannelHandlerContext ctx, HandshakeComplete handshake, String requestUri) throws URISyntaxException {
        QueryStringDecoder queryDecoder = new QueryStringDecoder(requestUri);
        Map<String, List<String>> parameters = queryDecoder.parameters();

        String appName = null;
        List<String> appNameList = parameters.get(URIConstans.APP_NAME);
        if (appNameList != null && !appNameList.isEmpty()) {
            appName = appNameList.get(0);
        }

        // generate a random agent id
        String id = null;
        if (appName != null) {
            // 如果有传 app name，则生成带 app name前缀的id，方便管理
            id = appName + "_" + RandomStringUtils.random(20, true, true).toUpperCase();
        } else {
            id = RandomStringUtils.random(20, true, true).toUpperCase();
        }
        // agent传过来，则优先用 agent的
        List<String> idList = parameters.get(URIConstans.ID);
        if (idList != null && !idList.isEmpty()) {
            id = idList.get(0);
        }

        String arthasVersion = null;
        List<String> arthasVersionList = parameters.get(URIConstans.ARTHAS_VERSION);
        if (arthasVersionList != null && !arthasVersionList.isEmpty()) {
            arthasVersion = arthasVersionList.get(0);
        }

        final String finalId = id;

        // URI responseUri = new URI("response", null, "/", "method=" + MethodConstants.AGENT_REGISTER + "&id=" + id, null);
        URI responseUri = UriComponentsBuilder.newInstance().scheme(URIConstans.RESPONSE).path("/")
                .queryParam(URIConstans.METHOD, MethodConstants.AGENT_REGISTER).queryParam(URIConstans.ID, id).build()
                .encode().toUri();

        AgentInfo info = new AgentInfo();

        // 前面可能有nginx代理
        HttpHeaders headers = handshake.requestHeaders();
        String host = HttpUtils.findClientIP(headers);

        if (host == null) {
            SocketAddress remoteAddress = ctx.channel().remoteAddress();
            if (remoteAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
                info.setHost(inetSocketAddress.getHostString());
                info.setPort(inetSocketAddress.getPort());
            }
        } else {
            info.setHost(host);
            Integer port = HttpUtils.findClientPort(headers);
            if (port != null) {
                info.setPort(port);
            }
        }

        info.setChannelHandlerContext(ctx);
        if (arthasVersion != null) {
            info.setArthasVersion(arthasVersion);
        }

        tunnelServer.addAgent(id, info);
        ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                tunnelServer.removeAgent(finalId);
            }

        });

        ctx.channel().writeAndFlush(new TextWebSocketFrame(responseUri.toString()));
    }

    private void openTunnel(ChannelHandlerContext ctx, String clientConnectionId) {
        Optional<ClientConnectionInfo> infoOptional = this.tunnelServer.findClientConnection(clientConnectionId);

        if (infoOptional.isPresent()) {
            ClientConnectionInfo info = infoOptional.get();
            logger.info("openTunnel clientConnectionId:" + clientConnectionId);

            Promise<Channel> promise = info.getPromise();
            promise.setSuccess(ctx.channel());
        } else {
            logger.error("Can not find client connection by id: {}", clientConnectionId);
        }

    }

    public static boolean validate(final String ip) {
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}
