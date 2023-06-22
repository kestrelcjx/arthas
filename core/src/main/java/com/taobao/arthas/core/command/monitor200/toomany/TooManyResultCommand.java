package com.taobao.arthas.core.command.monitor200.toomany;

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

@Name("toomany")
@Summary("Display sql and thread stack for fetch toomany resut from mysql")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  toomany -c 500 -s 10240000 -e 60000\n" +
        Constants.WIKI + Constants.WIKI_HOME + "toomany")
public class TooManyResultCommand extends EnhancerCommand {
    private static String className;
    private static String methodName;
    static {
        className = "com.mysql.jdbc.StatementImpl";
        methodName = "executeQuery|execute";
    }

    private int count = 1000;
    private int size = 100;
    private int elapsed = 10000;

    @Option(shortName = "c", longName = "count")
    @Description("Threshold of count")
    public void setCount(int count) {
        this.count = count;
    }

    @Option(shortName = "s", longName = "size")
    @Description("Threshold of size")
    public void setSize(int size) {
        this.size = size;
    }

    @Option(shortName = "e", longName = "elapsed")
    @Description("Threshold of elapsed time")
    public void setElapsed(int elapsed) {
        this.elapsed = elapsed;
    }
    public int getElapsed() {
        return elapsed;
    }
    public int getCount() {
        return count;
    }

    public int getSize() {
        return size;
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
            methodNameMatcher = SearchUtils.classNameMatcher(methodName, true);
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new TooManyResultAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
