package com.taobao.arthas.core.command.monitor200.bigkey;

import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

public interface MethodFilter {
    boolean ACCEPT = true;
    boolean REJECT = false;
    boolean accept(ClassNode classNode, MethodNode methodNode);
}
