package com.taobao.arthas.core.command.monitor200.jetty;

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
import com.taobao.middleware.cli.annotations.Summary;

import java.util.Arrays;

@Name("jetty")
@Summary("jetty http connection")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  jetty\n" +
        Constants.WIKI + Constants.WIKI_HOME + "jetty")
public class JettyCommand extends EnhancerCommand {
    private static String className;
    private static String methodName;
    static {
        className = "org.eclipse.jetty.http.HttpGenerator";
        methodName = "isPersistent";
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
        return new JettyAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
