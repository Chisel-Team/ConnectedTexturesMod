package team.chisel.ctm.client.util;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import net.minecraft.profiler.Profiler;

public class ProfileUtil {
    
    /** Will never be "on" so calls to it will short-circuit */
    private static final IProfiler dummyProfiler = new Profiler(0, () -> 0, false);
    
    private static ThreadLocal<IProfiler> profiler = ThreadLocal.withInitial(() -> {
        if (Thread.currentThread().getId() == 1) {
            return Minecraft.getInstance().getProfiler();
        } else {
            return dummyProfiler;
        }
    });
    
    public static void start(@Nonnull String section) {
        profiler.get().startSection(section);
    }
    
    public static void end() {
        profiler.get().endSection();
    }
    
    public static void endAndStart(@Nonnull String section) {
        profiler.get().endStartSection(section);
    }
}
