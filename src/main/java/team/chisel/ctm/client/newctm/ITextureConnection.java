package team.chisel.ctm.client.newctm;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface ITextureConnection {

    boolean ignoreStates();

    boolean connectTo(ConnectionCheck ctm, BlockState from, BlockState to, Direction dir);

    Optional<Boolean> connectInside();

    default ConnectionCheck applyTo(ConnectionCheck check) {
        check.ignoreStates(ignoreStates())
              .stateComparator(this::connectTo);
        check.disableObscuredFaceCheck = connectInside();
        return check;
    }
}