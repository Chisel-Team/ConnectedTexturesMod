package team.chisel.ctm.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

public class ProfileUtil {
    
    private static ThreadLocal<ProfilerFiller> profiler = ThreadLocal.withInitial(() -> {
        if (Thread.currentThread().getId() == 1) {
            return Minecraft.getInstance().getProfiler();
        } else {
            return InactiveProfiler.INSTANCE;
        }
    });
    
    public static void start(@NotNull String section) {
        profiler.get().push(section);
    }
    
    public static void end() {
        profiler.get().pop();
    }
    
    public static void endAndStart(@NotNull String section) {
        profiler.get().popPush(section);
    }
}
