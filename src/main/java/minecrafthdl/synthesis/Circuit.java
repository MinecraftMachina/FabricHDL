package minecrafthdl.synthesis;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Francis on 10/28/2016.
 */
public class Circuit {

    public static boolean TEST = false;

    ArrayList<ArrayList<ArrayList<BlockState>>> blocks;
    HashMap<Vec3i, String[]> te_map = new HashMap<Vec3i, String[]>();

    public Circuit(int sizeX, int sizeY, int sizeZ){
        this.blocks = new ArrayList<ArrayList<ArrayList<BlockState>>>();
        for (int x = 0; x < sizeX; x++) {
            this.blocks.add(new ArrayList<ArrayList<BlockState>>());
            for (int y = 0; y < sizeY; y++) {
                this.blocks.get(x).add(new ArrayList<BlockState>());
                for (int z = 0; z < sizeZ; z++) {
                    if (!Circuit.TEST) this.blocks.get(x).get(y).add(Blocks.AIR.getDefaultState());
                }
            }
        }
    }



    public void setBlock(int x, int y, int z, BlockState blockstate) {
        if (TEST) return;
        this.blocks.get(x).get(y).set(z, blockstate);
    }

    public void placeInWorld(World worldIn, BlockPos pos, Direction direction) {
        int width = blocks.size();
        int height = blocks.get(0).size();
        int length = blocks.get(0).get(0).size();

        int start_x = pos.getX();
        int start_y = pos.getY();
        int start_z = pos.getZ();

        if (direction == Direction.NORTH){
            start_z += 2;
        } else if (direction == Direction.SOUTH) {
            start_z -= length + 1;
        } else if (direction == Direction.EAST){
            start_x -= width + 1;
        } else if (direction == Direction.WEST) {
            start_x -= width + 1;
        }

        int y = start_y - 1;
        for (int z = start_z - 1; z < start_z + length + 1; z ++){
            for (int x = start_x - 1; x < start_x + width + 1; x++){
                worldIn.setBlockState(new BlockPos(x, y, z), Blocks.STONE_BRICKS.getDefaultState());
            }
        }

        HashMap<Vec3i ,BlockState> torches = new HashMap<Vec3i, BlockState>();

        for (int i = 0; i < width; i++){
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < length; k++) {
                    BlockState blockState = this.getState(i, j, k).getBlock().getDefaultState();
                    if (blockState == Blocks.REDSTONE_WALL_TORCH.getDefaultState() || blockState == Blocks.REDSTONE_TORCH.getDefaultState()) {
                        torches.put(new Vec3i(i, j, k), this.getState(i, j, k));
                    } else {
                        BlockPos blk_pos = new BlockPos(start_x + i, start_y + j, start_z + k);
                        worldIn.setBlockState(blk_pos, this.getState(i, j, k));

                        String[] te = this.te_map.get(new Vec3i(i, j, k));
                        if (te != null) {
                            worldIn.removeBlockEntity(blk_pos);
                            SignBlockEntity entity = new SignBlockEntity(blk_pos, Blocks.OAK_SIGN.getDefaultState());
                            for (int i1 = 0; i1 < te.length; i1++) {
                                entity.setTextOnRow(i1, new LiteralText(te[i1]));
                            }
                            worldIn.addBlockEntity(entity);
                        }
                    }
                }
            }
        }

        for (Map.Entry<Vec3i, BlockState> set : torches.entrySet()){
            worldIn.setBlockState(new BlockPos(start_x + set.getKey().getX(), start_y + set.getKey().getY(), start_z + set.getKey().getZ()), set.getValue());
        }
    }

    public int getSizeX() {
        return this.blocks.size();
    }

    public int getSizeY() {
        return this.blocks.get(0).size();
    }

    public int getSizeZ() {
        return this.blocks.get(0).get(0).size();
    }

    public BlockState getState(int x, int y, int z){
        return this.blocks.get(x).get(y).get(z);
    }

    public void insertCircuit(int x_offset, int y_offset, int z_offset, Circuit c) {
        for (int x = 0; x < c.getSizeX(); x++) {
            for (int y = 0; y < c.getSizeY(); y++) {
                for (int z = 0; z < c.getSizeZ(); z++) {
                    this.setBlock(x + x_offset, y + y_offset, z + z_offset, c.getState(x, y, z));

                    String[] te = c.te_map.get(new Vec3i(x, y, z));
                    if (te != null) {
                        this.te_map.put(new Vec3i(x + x_offset, y + y_offset, z + z_offset), te);
                    }
                }
            }
        }
    }
}
