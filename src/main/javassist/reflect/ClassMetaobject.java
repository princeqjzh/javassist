/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist.reflect;

import java.lang.reflect.*;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javassist.CtClass;

/**
 * A runtime class metaobject.
 *
 * <p>A <code>ClassMetaobject</code> is created for every
 * class of reflective objects.  It can be used to hold values
 * shared among the reflective objects of the same class.
 *
 * @see javassist.reflect.Metaobject
 */
public class ClassMetaobject implements Serializable {
    /**
     * The base-level methods controlled by a metaobject
     * are renamed so that they begin with
     * <code>methodPrefix "_m_"</code>.
     */
    static final String methodPrefix = "_m_";
    static final int methodPrefixLen = 3;

    private Class javaClass;
    private Constructor[] constructors;
    private Method[] methods;

    /**
     * Specifies how a <code>java.lang.Class</code> object is loaded.
     *
     * <p>If true, it is loaded by:
     * <ul><pre>Thread.currentThread().getContextClassLoader().loadClass()</pre></ul>
     * <p>If false, it is loaded by <code>Class.forName()</code>.
     * The default value is false.
     */
    public static boolean useContextClassLoader = false;

    /**
     * Constructs a <code>ClassMetaobject</code>.
     *
     * @param params    <code>params[0]</code> is the name of the class
     *                  of the reflective objects.
     */
    public ClassMetaobject(String[] params)
    {
        try {
            javaClass = getClassObject(params[0]);
        }
        catch (ClassNotFoundException e) {
            javaClass = null;
        }

        constructors = javaClass.getConstructors();
        methods = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(javaClass.getName());
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        javaClass = getClassObject(in.readUTF());
        constructors = javaClass.getConstructors();
        methods = null;
    }

    private Class getClassObject(String name) throws ClassNotFoundException {
        if (useContextClassLoader)
            return Thread.currentThread().getContextClassLoader()
                   .loadClass(name);
        else
            return Class.forName(name);
    }

    /**
     * Obtains the <code>java.lang.Class</code> representing this class.
     */
    public final Class getJavaClass() {
        return javaClass;
    }

    /**
     * Obtains the name of this class.
     */
    public final String getName() {
        return javaClass.getName();
    }

    /**
     * Returns true if <code>obj</code> is an instance of this class.
     */
    public final boolean isInstance(Object obj) {
        return javaClass.isInstance(obj);
    }

    /**
     * Creates a new instance of the class.
     *
     * @param args              the arguments passed to the constructor.
     */
    public final Object newInstance(Object[] args)
        throws CannotCreateException
    {
        int n = constructors.length;
        for (int i = 0; i < n; ++i) {
            try {
                return constructors[i].newInstance(args);
            }
            catch (IllegalArgumentException e) {
                // try again
            }
            catch (InstantiationException e) {
                throw new CannotCreateException(e);
            }
            catch (IllegalAccessException e) {
                throw new CannotCreateException(e);
            }
            catch (InvocationTargetException e) {
                throw new CannotCreateException(e);
            }
        }

        throw new CannotCreateException("no constructor matches");
    }

    /**
     * Is invoked when <code>static</code> fields of the base-level
     * class are read and the runtime system intercepts it.
     * This method simply returns the value of the field.
     *
     * <p>Every subclass of this class should redefine this method.
     */
    public Object trapFieldRead(String name) {
        Class jc = getJavaClass();
        try {
            return jc.getField(name).get(null);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Is invoked when <code>static</code> fields of the base-level
     * class are modified and the runtime system intercepts it.
     * This method simply sets the field to the given value.
     *
     * <p>Every subclass of this class should redefine this method.
     */
    public void trapFieldWrite(String name, Object value) {
        Class jc = getJavaClass();
        try {
            jc.getField(name).set(null, value);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Invokes a method whose name begins with
     * <code>methodPrefix "_m_"</code> and the identifier.
     *
     * @exception CannotInvokeException         if the invocation fails.
     */
    static public Object invoke(Object target, int identifier, Object[] args)
        throws Throwable
    {
        Method[] allmethods = target.getClass().getMethods();
        int n = allmethods.length;
        String head = methodPrefix + identifier;
        for (int i = 0; i < n; ++i)
            if (allmethods[i].getName().startsWith(head)) {
                try {
                    return allmethods[i].invoke(target, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getTargetException();
                } catch (java.lang.IllegalAccessException e) {
                    throw new CannotInvokeException(e);
                }
            }

        throw new CannotInvokeException("cannot find a method");
    }

    /**
     * Is invoked when <code>static</code> methods of the base-level
     * class are called and the runtime system intercepts it.
     * This method simply executes the intercepted method invocation
     * with the original parameters and returns the resulting value.
     *
     * <p>Every subclass of this class should redefine this method.
     */
    public Object trapMethodcall(int identifier, Object[] args) 
        throws Throwable
    {
        try {
            Method[] m = getReflectiveMethods();
            return m[identifier].invoke(null, args);
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
        catch (java.lang.IllegalAccessException e) {
            throw new CannotInvokeException(e);
        }
    }

    /**
     * Returns an array of the methods defined on the given reflective
     * object.  This method is for the internal use only.
     */
    public final Method[] getReflectiveMethods() {
        if (methods != null)
            return methods;

        Class baseclass = getJavaClass();
        Method[] allmethods = baseclass.getMethods();
        int n = allmethods.length;
        methods = new Method[n];
        for (int i = 0; i < n; ++i) {
            Method m = allmethods[i];
            if (m.getDeclaringClass() == baseclass) {
                String mname = m.getName();
                if (mname.startsWith(methodPrefix)) {
                    int k = 0;
                    for (int j = methodPrefixLen;; ++j) {
                        char c = mname.charAt(j);
                        if ('0' <= c && c <= '9')
                            k = k * 10 + c - '0';
                        else
                            break;
                    }

                    methods[k] = m;
                }
            }
        }

        return methods;
    }

    /**
     * Returns the name of the method specified
     * by <code>identifier</code>.
     */
    public final String getMethodName(int identifier) {
        String mname = getReflectiveMethods()[identifier].getName();
        int j = ClassMetaobject.methodPrefixLen;
        for (;;) {
            char c = mname.charAt(j++);
            if (c < '0' || '9' < c)
                break;
        }

        return mname.substring(j);
    }

    /**
     * Returns an array of <code>Class</code> objects representing the
     * formal parameter types of the method specified
     * by <code>identifier</code>.
     */
    public final Class[] getParameterTypes(int identifier) {
        return getReflectiveMethods()[identifier].getParameterTypes();
    }

    /**
     * Returns a <code>Class</code> objects representing the
     * return type of the method specified by <code>identifier</code>.
     */
    public final Class getReturnType(int identifier) {
        return getReflectiveMethods()[identifier].getReturnType();
    }
}