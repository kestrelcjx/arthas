package com.taobao.arthas.core.command.monitor200.bigkey;

import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.util.ArthasCheckUtils;
import com.taobao.arthas.core.util.matcher.Matcher;

public class Enhancer4Bigkey extends Enhancer {
    public Enhancer4Bigkey(AdviceListener listener, boolean isTracing, boolean skipJDKTrace, Matcher classNameMatcher, Matcher classNameExcludeMatcher, Matcher methodNameMatcher) {
        super(listener, isTracing, skipJDKTrace, classNameMatcher, classNameExcludeMatcher, methodNameMatcher);
    }

    @Override
    protected boolean isIgnore(MethodNode methodNode, Matcher methodNameMatcher) {
        return null == methodNode || isAbstract(methodNode.access) || !methodNameMatcher.matching(methodNode)
                || ArthasCheckUtils.isEquals(methodNode.name, "<clinit>");
    }
}
