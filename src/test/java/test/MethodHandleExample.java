package test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Random;

import org.openjdk.jmh.infra.Blackhole;

public class MethodHandleExample
{
    private static final Random random = new Random();
    private static final boolean BIND_METHOD_HANDLE = true;

    public static class Endpoint
    {
        public void onOpen(byte[] bytes)
        {
            Blackhole.consumeCPU(100);
        }
    }

    public static void main(String[] args) throws Throwable
    {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = Endpoint.class.getMethod("onOpen", byte[].class);
        MethodHandle methodHandle = lookup.unreflect(method);

        while (true)
        {
            Endpoint endpoint = new Endpoint();
            MethodHandle specificHandle = BIND_METHOD_HANDLE ? methodHandle.bindTo(endpoint) : methodHandle;

            for (int i = 0; i < 1000; i++)
            {
                byte[] bytes = new byte[random.nextInt(99) + 1];
                random.nextBytes(bytes);

                if (BIND_METHOD_HANDLE)
                    specificHandle.invoke(bytes);
                else
                    methodHandle.invoke(endpoint, bytes);
            }
        }
    }
}
