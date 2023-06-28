package com.taobao.arthas.core.command.monitor200.tomcat;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
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

@Name("keepalive")
@Summary("keepalive http connection for cxf")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  keepalive -host 127.0.0.1 -left 6\n" +
        Constants.WIKI + Constants.WIKI_HOME + "keepalive")
public class KeepAliveCommand extends EnhancerCommand {
    private static final Logger logger = LoggerFactory.getLogger(KeepAliveCommand.class);
    private static String className;
    private static String methodName;
    static {
        className = "org.apache.tomcat.util.net.SocketWrapper";
        methodName = "decrementKeepAlive";
    }

    protected String host = "hello world";
    @Option(shortName = "host", longName = "host")
    @Description("host ip address")
    public void setHost(String host) {
        this.host = host;
        logger.info("{}", host);
    }

    private int left;
    @Option(shortName = "left", longName = "left")
    @Description("keepalive left")
    public void setCount(int left) {
        this.left = left;
    }

    public int getLeft() {
        return left;
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
        return new KeepAliveAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
