package fabrichdl;

import GraphBuilder.GraphBuilder;
import minecrafthdl.synthesis.Circuit;
import minecrafthdl.synthesis.IntermediateCircuit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FabricHDL implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("FabricHDL");

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("synth")
                    .then(argument("file", string())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                ServerWorld world = context.getSource().getWorld();
                                String file = context.getArgument("file", String.class);
                                synthGen(world, player.getBlockPos(), file);
                                return 1;
                            })
                    )
            );
        });
    }

    private void synthGen(World worldIn, BlockPos pos, String file) {
        try {
            IntermediateCircuit ic = new IntermediateCircuit();
            ic.loadGraph(GraphBuilder.buildGraph(file));
            ic.buildGates();
            ic.routeChannels();
            Circuit circuit = ic.genCircuit();
            circuit.placeInWorld(worldIn, pos, Direction.NORTH);
        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendChatMessage(
                    "An error occurred while generating the circuit, check the logs! Sorry!");
            e.printStackTrace();
        }
    }
}
