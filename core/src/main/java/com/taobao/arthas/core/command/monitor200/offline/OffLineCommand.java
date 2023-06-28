package com.taobao.arthas.core.command.monitor200.offline;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.monitor200.EnhancerCommand;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@Name("offline")
@Summary("offline http connection for cxf")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  offline -host 127.0.0.1 -off true -close false\n" +
        Constants.WIKI + Constants.WIKI_HOME + "offline")
public class OffLineCommand extends EnhancerCommand {
    private static final Logger logger = LoggerFactory.getLogger(OffLineCommand.class);
    private static String className;
    private static String methodName;
    static {
        className = "org.apache.cxf.transport.servlet.AbstractHTTPServlet";
        methodName = "service";
    }

    protected ConcurrentHashMap<String,String> hosts = new ConcurrentHashMap<>();
    @Option(shortName = "host", longName = "host")
    @Description("host ip address")
    public void setHost(String host) {
        this.hosts.put(host,host);
        logger.info("{}", JSON.toJSONString(hosts));
    }

    @Option(shortName = "off", longName = "off")
    @Description("offline or online")
    public void setOff(boolean off) {
        if(!off){
            hosts.clear();
        }
    }

    @Option(shortName = "className", longName = "className")
    @Description("className")
    public void setClassName(String cls) {
        className = cls;
    }

    @Option(shortName = "method", longName = "method")
    @Description("method")
    public void setMethod(String method) {
        methodName = method;
    }

    private boolean close;
    @Option(shortName = "close", longName = "close")
    @Description("close socket")
    public void setClose(boolean close) {
        this.close = close;
    }

    public boolean isClose() {
        return close;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(className, false);
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(methodName, false);
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new OfflineAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
