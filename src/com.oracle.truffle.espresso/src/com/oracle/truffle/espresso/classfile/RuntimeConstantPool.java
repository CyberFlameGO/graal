/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.espresso.classfile;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.espresso.impl.Field;
import com.oracle.truffle.espresso.impl.Klass;
import com.oracle.truffle.espresso.impl.Method;
import com.oracle.truffle.espresso.impl.ObjectKlass;
import com.oracle.truffle.espresso.runtime.EspressoContext;
import com.oracle.truffle.espresso.runtime.StaticObject;

public final class RuntimeConstantPool extends ConstantPool {

    private final EspressoContext context;
    private final ConstantPool pool;
    private final StaticObject classLoader;

    @CompilationFinal(dimensions = 1) //
    private final Resolvable.ResolvedConstant[] constants;

    public RuntimeConstantPool(EspressoContext context, ConstantPool pool, StaticObject classLoader) {
        this.context = context;
        this.pool = pool;
        constants = copyResolvedConstant(pool); // utf8, int, floats..., others->null
        this.classLoader = classLoader;
    }

    private static Resolvable.ResolvedConstant[] copyResolvedConstant(ConstantPool pool) {
        return new Resolvable.ResolvedConstant[pool.length()];
    }

    @Override
    public int length() {
        return pool.length();
    }

    @Override
    public PoolConstant at(int index, String description) {
        return pool.at(index, description);
    }

    private Resolvable.ResolvedConstant resolvedAt(Klass accessingKlass, int index, String description) {
        Resolvable.ResolvedConstant c = constants[index];
        if (c == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            synchronized (this) {
                // fence += 1;
                // FIXME(peterssen): Add memory fence for array read.
                c = constants[index];
                if (c == null) {
                    constants[index] = c = ((Resolvable) pool.at(index, description)).resolve(this, index, accessingKlass);
                }
            }
        }
        return c;
    }

    public StaticObject resolvedStringAt(int index) {
        Resolvable.ResolvedConstant resolved = resolvedAt(null, index, "string");
        return (StaticObject) resolved.value();
    }

    public Klass resolvedKlassAt(Klass accessingKlass, int index) {
        Resolvable.ResolvedConstant resolved = resolvedAt(accessingKlass, index, "klass");
        return (Klass) resolved.value();
    }

    public Field resolvedFieldAt(Klass accessingKlass, int index) {
        Resolvable.ResolvedConstant resolved = resolvedAt(accessingKlass, index, "field");
        return (Field) resolved.value();
    }

    public Method resolvedMethodAt(Klass accessingKlass, int index) {
        Resolvable.ResolvedConstant resolved = resolvedAt(accessingKlass, index, "method");
        return (Method) resolved.value();
    }

    public StaticObject resolvedMethodHandleAt(Klass accessingKlass, int index) {
        Resolvable.ResolvedConstant resolved = resolvedAt(accessingKlass, index, "method handle");
        return (StaticObject) resolved.value();
    }

    public StaticObject resolvedMethodTypeAt(Klass accessingKlass, int index) {
        Resolvable.ResolvedConstant resolved = resolvedAt(accessingKlass, index, "method type");
        return (StaticObject) resolved.value();
    }

    public StaticObject getClassLoader() {
        return classLoader;
    }

    public EspressoContext getContext() {
        return context;
    }

    public void setKlassAt(int index, ObjectKlass klass) {
        constants[index] = new ClassConstant.Resolved(klass);
    }

    @Override
    public int getMajorVersion() {
        return pool.getMajorVersion();
    }
}
