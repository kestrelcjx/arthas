package com.taobao.arthas.core.command.monitor200.bigkey;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.core.util.matcher.Matcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JedisMethodFilter implements MethodFilter, Matcher<MethodNode> {
    private static final Logger logger = LoggerFactory.getLogger(JedisMethodFilter.class);
    private Set<String> excludeMethodNames = new HashSet<>(16);
    public JedisMethodFilter(){
        excludeMethodNames.addAll(Arrays.asList("clone", "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait"));
        excludeMethodNames.addAll(Arrays.asList("<init>","ping", "close", "disconnect", "resetState", "getClient", "setClient", "getDB", "isInMulti", "isInWatch", "clear",
                "pipelined", "setPassword", "setDb", "setDataSource", "getClusterNodes", "getConnectionFromSlot"));
    }

    @Override
    public boolean accept(ClassNode classNode, MethodNode methodNode) {
        if( (methodNode.access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT ||
                (methodNode.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC ||
                (methodNode.access & Opcodes.ACC_PUBLIC) != Opcodes.ACC_PUBLIC ||
                ((methodNode.access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE)){
            logger.info(methodNode.name + " is excluded.");

            return REJECT;
        }

        String methodName = methodNode.name;
        if(excludeMethodNames.contains(methodName)){
            logger.info(methodName + " is excluded.");

            return REJECT;
        }

        if(methodName.contains("PINPOINT")){
            return REJECT;
        }

        return ACCEPT;
    }

    @Override
    public boolean matching(MethodNode target) {
        return accept(null,target);
    }
}
