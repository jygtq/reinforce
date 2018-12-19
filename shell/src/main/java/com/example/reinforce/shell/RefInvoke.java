package com.example.reinforce.shell;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * 作者：created by wujin on 2018/12/15
 * 邮箱：jin.wu@geely.com
 * 反射工具
 */
public class RefInvoke {

    /**
     * 调用指定类中某个静态方法
     * @param className 类名
     * @param methodName 方法名
     * @param pareType 参数类型
     * @param pareValues 参数值
     * @return
     */
    public static Object invokeStaticMethod(String className, String methodName, Class[] pareType, Object[] pareValues) {
        try {
            Class objClass = Class.forName(className);
            Method method = objClass.getDeclaredMethod(methodName, pareType);
            method.setAccessible(true);
            return method.invoke(null, pareValues);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;


    }

    /**
     * 调用指定类中某个方法
     * @param className 类名
     * @param methodName 方法名
     * @param pareType 参数类型
     * @param pareValues 参数值
     * @return
     */
    public static Object invokeMethod(String className, String methodName, Object obj, Class[] pareType, Object[] pareValues) {
        try {
            Class objClass = Class.forName(className);
            Method method = objClass.getDeclaredMethod(methodName, pareType);
            method.setAccessible(true);
            return method.invoke(obj, pareValues);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();

        } catch (InvocationTargetException e) {
            e.printStackTrace();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;


    }


    /**
     * 通过反射获取指定类中某个属性
     * @param className
     * @param obj
     * @param filedName
     * @return
     */
    public static Object getFieldObject(String className, Object obj, String filedName) {

        try {
            Class objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;


    }

    /**
     * 反射修改指定类中某个属性
     * @param classname 类名
     * @param filedName 属性名
     * @param obj 类对象
     * @param filedValue 属性值
     */
    public static void setFieldObject(String classname, String filedName, Object obj, Object filedValue) {

        try {

            Class objClass = Class.forName(classname);
            Field field = objClass.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(obj, filedValue);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();

        }

    }

}
