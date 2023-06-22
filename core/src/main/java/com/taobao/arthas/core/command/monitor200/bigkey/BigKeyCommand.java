package com.taobao.arthas.core.command.monitor200.bigkey;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.monitor200.EnhancerCommand;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;

@Name("bigkey")
@Summary("Display redis command and thread stack for fetch bigkey from redis")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  bigkey -ws 1024 -rs 10240 -e 10 \n" +
        Constants.WIKI + Constants.WIKI_HOME + "bigkey")
public class BigKeyCommand extends EnhancerCommand {
    private static final Logger logger = LoggerFactory.getLogger(BigKeyCommand.class);

    private static String className;
    private static String methodName;
    static {
        className = "redis.clients.jedis.Protocol";
        methodName = "sendCommand|read";
    }

    private int writeSize = 1024;
    private int readSize = 10240;
    private int elapsed = 5;
    private boolean throwable = true;

    @Option(shortName = "ws", longName = "writesize")
    @Description("Threshold of write size to redis")
    public void setWriteSize(int writeSize) {
        this.writeSize = writeSize;
    }

    @Option(shortName = "rs", longName = "readsize")
    @Description("Threshold of read size from redis")
    public void setReadSize(int readSize) {
        this.readSize = readSize;
    }

    @Option(shortName = "e", longName = "elapsed")
    @Description("Threshold of elapsed time")
    public void setElapsed(int elapsed) {
        this.elapsed = elapsed;
    }

    @Option(shortName = "t", longName = "throwable")
    @Description("print throwable ?true:print,false:do not print")
    public void setThrowable(boolean throwable) {
        this.throwable = throwable;
    }
    public int getWriteSize() {
        return writeSize;
    }
    public int getReadSize() {
        return readSize;
    }
    public int getElapsed() {
        return elapsed;
    }

    public boolean isThrowable() {
        return throwable;
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
        return new BigKeyAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }
    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
    @Override
    protected void enhance(CommandProcess process) {
        try {
            this.init(process);
        }catch (Throwable t){
            logger.error("",t);
        }
        super.enhance(process);
    }

    private void init(CommandProcess process) throws UnmodifiableClassException {
        Session session = process.session();
        Instrumentation inst = session.getInstrumentation();

        String classPattern = "redis.clients.jedis.BinaryJedis|" +
                "redis.clients.jedis.Jedis|" +
                "redis.clients.jedis.Pipeline|" +
                "redis.clients.jedis.PipelineBase|" +
                "redis.clients.jedis.MultiKeyPipelineBase";

        JedisMethodInterceptor.bigKeyCommand = this;
        JedisMethodFilter jedisMethodFilter = new JedisMethodFilter();
        JedisMethodAdviceListener listener = new JedisMethodAdviceListener();
        Enhancer4Bigkey enhancer = new Enhancer4Bigkey(listener, false, false,
                SearchUtils.classNameMatcher(classPattern, true),
                null, jedisMethodFilter);

        process.register(listener, enhancer);
        enhancer.enhance(inst,1000);
    }
}
