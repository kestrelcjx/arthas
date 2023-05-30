package com.alibaba.arthas.tunnel.server.app.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.arthas.tunnel.server.utils.InetAddressUtil;
import com.taobao.arthas.common.ArthasConstants;

/**
 *
 * @author hengyunabc 2019-08-29
 *
 */
@Component
@ConfigurationProperties(prefix = "arthas")
public class ArthasProperties {
    @Value("${server.port}")
    private int serverPort;

    private Server server;

    private EmbeddedRedis embeddedRedis;

    private boolean enableDetailPages = false;

    private boolean enableIframeSupport = true;

    private boolean enableHttpPort = false;

    private boolean enableStatUrl = false;

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public EmbeddedRedis getEmbeddedRedis() {
        return embeddedRedis;
    }

    public void setEmbeddedRedis(EmbeddedRedis embeddedRedis) {
        this.embeddedRedis = embeddedRedis;
    }

    public boolean isEnableDetailPages() {
        return enableDetailPages;
    }

    public void setEnableDetailPages(boolean enableDetailPages) {
        this.enableDetailPages = enableDetailPages;
    }

    public boolean isEnableIframeSupport() {
        return enableIframeSupport;
    }

    public void setEnableIframeSupport(boolean enableIframeSupport) {
        this.enableIframeSupport = enableIframeSupport;
    }

    public boolean isEnableHttpPort() {
        return enableHttpPort;
    }

    public void setEnableHttpPort(boolean enableHttpPort) {
        this.enableHttpPort = enableHttpPort;
    }

    public boolean isEnableStatUrl() {
        return enableStatUrl;
    }

    public void setEnableStatUrl(boolean enableStatUrl) {
        this.enableStatUrl = enableStatUrl;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public static class Server {
        /**
         * tunnel server listen host
         */
        private String host;
        private int port;
        private boolean ssl;
        private String path = ArthasConstants.DEFAULT_WEBSOCKET_PATH;

        /**
         * 客户端连接的地址。也用于保存到redis里，当部署tunnel server集群里需要。不配置则会自动获取
         */
        private String clientConnectHost = InetAddressUtil.getInetAddress();

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isSsl() {
            return ssl;
        }

        public void setSsl(boolean ssl) {
            this.ssl = ssl;
        }

        public String getClientConnectHost() {
            return clientConnectHost;
        }

        public void setClientConnectHost(String clientConnectHost) {
            this.clientConnectHost = clientConnectHost;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

    }

    /**
     * for test
     *
     * @author hengyunabc 2020-11-03
     *
     */
    public static class EmbeddedRedis {
        private boolean enabled = false;
        private String host = "127.0.0.1";
        private int port = 6379;
        private List<String> settings = new ArrayList<String>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public List<String> getSettings() {
            return settings;
        }

        public void setSettings(List<String> settings) {
            this.settings = settings;
        }
    }

}
