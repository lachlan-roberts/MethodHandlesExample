package test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class MethodHandlesBenchmark
{
    private static final Random random = new Random();

    @Param({"BOUND_INVOKE", "UNBOUND_INVOKE"})
    public static String STRATEGY;

    public static class Endpoint
    {
        public void onOpen(byte[] bytes)
        {
            Blackhole.consumeCPU(100);
        }
    }

    @Benchmark
    public void test() throws Throwable
    {
        // Generate the base MethodHandle (we store this in a MetaData map and reuse every time).
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = Endpoint.class.getMethod("onOpen", byte[].class);
        MethodHandle methodHandle = lookup.unreflect(method);

        for (int i = 0; i < 5; i++)
        {
            // When we open a connection we bind the MethodHandle to a new Endpoint.
            Endpoint endpoint = new Endpoint();
            MethodHandle boundMethodHandle = methodHandle;
            if (STRATEGY.equals("BOUND_INVOKE"))
                boundMethodHandle = methodHandle.bindTo(endpoint);
            for (int j = 0; j < 100; j++)
            {
                byte[] bytes = new byte[random.nextInt(99) + 1];
                random.nextBytes(bytes);

                // Invoke the MethodHandle specific to the Endpoint.
                switch (STRATEGY)
                {
                    case "UNBOUND_INVOKE":
                        methodHandle.invoke(endpoint, bytes);
                        break;
                    case "BOUND_INVOKE":
                        boundMethodHandle.invoke(bytes);
                        break;
                    default:
                        throw new IllegalStateException(STRATEGY);
                }
            }
        }
    }

    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder()
            .include(MethodHandlesBenchmark.class.getSimpleName())
            .warmupIterations(3)
            .measurementIterations(3)
            .forks(1)
            .threads(1)
            //.addProfiler(LinuxPerfProfiler.class)
            .build();

        new Runner(opt).run();
    }
}
