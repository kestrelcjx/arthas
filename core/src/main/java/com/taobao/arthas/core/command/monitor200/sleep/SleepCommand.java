package com.taobao.arthas.core.command.monitor200.sleep;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.monitor200.EnhancerCommand;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.*;

import java.util.Arrays;

@Name("sleep")
@Summary("sleep")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  sleep org.apache.commons.lang.StringUtils isBlank \n" +
        Constants.WIKI + Constants.WIKI_HOME + "sleep")
public class SleepCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;

    private int millis;
    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to watch")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to watch")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Option(shortName = "s", longName = "sleepMillis")
    @Description("sleep time")
    public void setMillis(int millis) {
        this.millis = millis;
    }

    public int getMillis() {
        return millis;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), false);
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), false);
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), false);
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new SleepAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
