package com.taobao.arthas.core.command.monitor200.blocked;

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

@Name("blocked")
@Summary("Display the blocked threads for long time")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  blocked -t 1000\n" +
        Constants.WIKI + Constants.WIKI_HOME + "blocked")
public class BlockedThreadCommand extends EnhancerCommand {
    private static String className;
    private static String methodName;
    static {
        className = "com.taobao.hsf.remoting.provider.ProviderProcessor|"+
                "org.springframework.web.servlet.DispatcherServlet|" +
                "com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently|" +
                "com.alibaba.rocketmq.client.consumer.listener.MessageListenerOrderly";
        methodName = "handleRequest|doDispatch|consumeMessage";
    }
    private int maxExeTime = 1000;

    @Option(shortName = "t", longName = "time")
    @Description("Threshold of execution time")
    public void setMaxExeTime(int maxExeTime) {
        this.maxExeTime = maxExeTime;
    }
    @Option(shortName = "s", longName = "stop")
    @Description("stop blocked thread monitor")
    public void setStop(boolean stop){
        if(stop){
            BlockedThreadChecker.getInstance().stop();
        }
    }
    public int getMaxExeTime() {
        return maxExeTime;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(className, true);
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
            methodNameMatcher = SearchUtils.classNameMatcher(methodName, true);
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new BlockedThreadAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
