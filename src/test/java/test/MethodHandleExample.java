package test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Random;

import org.openjdk.jmh.infra.Blackhole;

public class MethodHandleExample
{
    private static final Random random = new Random();

    public static class Endpoint
    {
        public void onOpen(byte[] bytes)
        {
            Blackhole.consumeCPU(100);
        }
    }

    public static void main(String[] args) throws Throwable
    {
        // Generate the base MethodHandle (we store this in a MetaData map and reuse every time).
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = Endpoint.class.getMethod("onOpen", byte[].class);
        MethodHandle methodHandle = lookup.unreflect(method);

        while (true)
        {
            // When we open a connection we bind the MethodHandle to a new Endpoint.
            Endpoint endpoint = new Endpoint();
            MethodHandle specificHandle = methodHandle.bindTo(endpoint);
            for (int i = 0; i < 1000; i++)
            {
                byte[] bytes = new byte[random.nextInt(99) + 1];
                random.nextBytes(bytes);

                // Invoke the MethodHandle bound to the endpoint.
                specificHandle.invoke(bytes);

                // If instead we invoke the MethodHandle without binding we avoid seeing spike on flamegraph.
                // But it is less efficient (see benchmark).
                // methodHandle.invoke(endpoint, bytes);
            }
        }
    }
}
