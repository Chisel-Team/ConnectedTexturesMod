package team.chisel.ctm.client.util;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.profiler.EmptyProfiler;
import net.minecraft.profiler.IProfiler;

public class ProfileUtil {
    
    private static ThreadLocal<IProfiler> profiler = ThreadLocal.withInitial(() -> {
        if (Thread.currentThread().getId() == 1) {
            return Minecraft.getInstance().getProfiler();
        } else {
            return EmptyProfiler.INSTANCE;            
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
