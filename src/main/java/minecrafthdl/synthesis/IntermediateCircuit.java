package minecrafthdl.synthesis;

import MinecraftGraph.*;
import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.routing.Channel;
import minecrafthdl.synthesis.routing.Net;
import minecrafthdl.synthesis.routing.Router;
import minecrafthdl.synthesis.routing.pins.GatePins;
import minecrafthdl.synthesis.routing.pins.PinsArray;
import minecrafthdl.testing.TestLogicGates;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Francis on 11/28/2016.
 */
public class IntermediateCircuit {

    public ArrayList<ArrayList<Vertex>> vertex_layers = new ArrayList<ArrayList<Vertex>>();
    public ArrayList<ArrayList<Gate>> gate_layers = new ArrayList<ArrayList<Gate>>();
    public ArrayList<Channel> channels = new ArrayList<Channel>();


    public void loadGraph(Graph graph) {
        System.out.println("Graph built");

        ArrayList<Vertex> finished = new ArrayList<Vertex>();
        ArrayList<Vertex> in_process = new ArrayList<Vertex>();
        ArrayList<Vertex> process_done = new ArrayList<Vertex>();
        ArrayList<Vertex> to_process = new ArrayList<Vertex>();

        for (Vertex v : graph.getVertices()) {
            if (v.getType() == VertexType.INPUT) {
                in_process.add(v);
            } else if (v.getType() == VertexType.FUNCTION){
                Function f = (Function) v;
                if (f.func_type == FunctionType.HIGH || f.func_type == FunctionType.LOW) in_process.add(v);
            }

        }

        int layer_num = 0;

        while (in_process.size() > 0){
            int size = in_process.size();
            vertex_layers.add(new ArrayList<Vertex>());
            for (Vertex v : in_process){
                boolean valid = true;
                for (Vertex p : v.getBefore()){
                    if (!finished.contains(p)) {
                        valid = false;
                        break;
                    }
                }

                if (valid){
                    vertex_layers.get(layer_num).add(v);
                    process_done.add(v);

                    for (Vertex n : v.getNext()){
                        to_process.add(n);
                    }
                }
            }
            for (Vertex v : process_done){
                in_process.remove(v);
                finished.add(v);
            }
            for (Vertex v : to_process){
                if (!in_process.contains(v)) in_process.add(v);
            }
            process_done.clear();
            to_process.clear();
            layer_num++;

        }

        ArrayList<Vertex> outputs_not_in_last = new ArrayList<Vertex>();

        for (ArrayList<Vertex> v_l : this.vertex_layers){
            if (this.vertex_layers.indexOf(v_l) == vertex_layers.size() - 1) break;
            ArrayList<Vertex> to_remove = new ArrayList<Vertex>();

            for (Vertex v : v_l){
                if (v.type == VertexType.OUTPUT) {
                    outputs_not_in_last.add(v);
                    to_remove.add(v);
                }
            }

            for (Vertex v : to_remove){
                v_l.remove(v);
            }
        }

        ArrayList<Vertex> last_layer = this.vertex_layers.get(this.vertex_layers.size() - 1);
        for (Vertex v : outputs_not_in_last){
            last_layer.add(v);
        }

        for (int i = 0; i < vertex_layers.size() - 1; i++){
            ArrayList<Vertex> layer = vertex_layers.get(i);
            ArrayList<Vertex> next_layer = vertex_layers.get(i+1);

            for (Vertex v : layer){
                ArrayList<Vertex> addToNext = new ArrayList<Vertex>();
                ArrayList<Vertex> removeFromNext = new ArrayList<Vertex>();

                for (Vertex next : v.getNext()){
                    if (!next_layer.contains(next)){
                        Vertex relay = new Function(1, FunctionType.RELAY, 1);

                        next_layer.add(relay);

                        removeFromNext.add(next);
                        next.removeBefore(v);

                        addToNext.add(relay);
                        relay.addToBefore(v);

                        relay.addToNext(next);
                        next.addToBefore(relay);

                        next.handleRelay(v, relay);
                    }
                }

                for (Vertex x : addToNext){
                    v.addToNext(x);
                }

                for (Vertex x : removeFromNext){
                    v.removeNext(x);
                }
            }
        }

    }

    public void printLayers(){
        for (ArrayList<Vertex> layer : vertex_layers){
            for (Vertex v : layer) {
                if (v.getType() == VertexType.INPUT){
                    System.out.print("I, ");
                }
                if (v.getType() == VertexType.FUNCTION){
                    System.out.print(((Function) v).getFunc_Type() + ", ");
                }
                if (v.getType() == VertexType.OUTPUT){
                    System.out.print("O, ");
                }
            }
            System.out.print("\n");
        }
    }

    public void buildGates() {
        if (this.vertex_layers.size() == 0) throw new MHDLException("Must load graph before building gates");

        for (ArrayList<Vertex> v_layer : this.vertex_layers) {
            ArrayList<Gate> this_layer = new ArrayList<Gate>();
            for (Vertex v : v_layer) {
                this_layer.add(genGate(v));
            }
            this.gate_layers.add(this_layer);
        }
    }

    public void routeChannels() {
        for (int i = 0; i < vertex_layers.size() - 1; i++){
            ArrayList<Vertex> top_vertices = vertex_layers.get(i);
            ArrayList<Gate> top_gates = gate_layers.get(i);
            ArrayList<Vertex> bottom_vertices = vertex_layers.get(i+1);
            ArrayList<Gate> bottom_gates = gate_layers.get(i + 1);

            Router.PinInitRtn rtn = Router.initializePins(top_vertices, top_gates, bottom_vertices, bottom_gates, 1);

            HashMap<Vertex, GatePins> pin_map = rtn.pin_map;
            PinsArray pins_array = rtn.pins_array;

            HashMap<Integer, Net> nets = Router.initializeNets(top_vertices, bottom_vertices, pin_map);

            this.channels.add(Router.placeNets(nets, pins_array));

            Net.num_nets = 0;
        }


    }

    public Circuit genCircuit(){
        if (this.gate_layers.size() == 0) throw new MHDLException("Must build gates before generating final circuit");

        int size_x = 0;
        int size_y = 0;
        int size_z = 0;

        int[] layers_size_z = new int[this.gate_layers.size()];

        for (ArrayList<Gate> layer : this.gate_layers){
            int this_size_x = layer.size() == 0 ? 0 : layer.size() - 1;
            int this_size_y = 0;
            int this_size_z = 0;

            for (Circuit c : layer){
                this_size_x += c.getSizeX();
                if (c.getSizeY() > this_size_y) this_size_y = c.getSizeY();
                if (c.getSizeZ() > this_size_z) this_size_z = c.getSizeZ();
            }

            if (this_size_x > size_x) size_x = this_size_x;
            if (this_size_y > size_y) size_y = this_size_y;
            size_z += this_size_z;

            layers_size_z[this.gate_layers.indexOf(layer)] = this_size_z;
        }

        if (size_y < 3) size_y = 3;

        for (Channel c : this.channels){
            if (c.sizeX() + 1 > size_x) size_x = c.sizeX() + 1;
            size_z += c.sizeZ() + 1;
        }

        Circuit circuit = new Circuit(size_x, size_y, size_z);

        int z_offset = 0;
        for (int i = 0; i < this.gate_layers.size(); i++) {
            int x_offset = 0;
            for (Gate g : this.gate_layers.get(i)){
                circuit.insertCircuit(x_offset, 0, z_offset, g);

                if (g.getSizeZ() - 1 < layers_size_z[i]) {
                    for (int z = g.getSizeZ(); z < layers_size_z[i]; z++){
                        if (z == layers_size_z[i] - 1) circuit.setBlock(x_offset, 0, z_offset + z, Blocks.REPEATER.getDefaultState().with(
                                Properties.HORIZONTAL_FACING, Direction.NORTH));
                        else circuit.setBlock(x_offset, 0, z_offset + z, Blocks.REDSTONE_WIRE.getDefaultState());
                    }
                }

                x_offset += 1 + g.getSizeX();
            }
            z_offset += layers_size_z[i];

            if (i < this.gate_layers.size() - 1) {
                Channel c = this.channels.get(i);
                circuit.insertCircuit(0, 0, z_offset, c.genChannelCircuit());
                z_offset += c.sizeZ();
            }
        }

        return circuit;
    }

    private static FunctionType getFunctionType(Vertex v) {

        if (v.getType() == VertexType.FUNCTION) {
            return ((Function) v).getFunc_Type();
        } else {
            if (v.getType() == VertexType.INPUT) return FunctionType.Input;
            else return FunctionType.Output;
        }

    }


    private static Gate genGate(Vertex v) {
        if (getFunctionType(v) == FunctionType.AND) {
            return Circuit.TEST? TestLogicGates.AND(((Function)v).get_num_inputs()) : LogicGates.AND(((Function)v).get_num_inputs());
        } else if ( getFunctionType(v) == FunctionType.OR){
            return Circuit.TEST? TestLogicGates.OR(((Function)v).get_num_inputs()) : LogicGates.OR(((Function)v).get_num_inputs());
        } else if ( getFunctionType(v) == FunctionType.INV){
            return Circuit.TEST? TestLogicGates.NOT() : LogicGates.NOT();
        } else if ( getFunctionType(v) == FunctionType.RELAY) {
            return Circuit.TEST ? TestLogicGates.RELAY() : LogicGates.RELAY();
        }else if ( getFunctionType(v) == FunctionType.XOR){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.XOR();
        }else if ( getFunctionType(v) == FunctionType.MUX){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.MUX();
        }
//        else if ( getFunctionType(v )== FunctionType.IO){
//            return Circuit.TEST? TestLogicGates.IO() : LogicGates.IO();
//        }
        else if ( getFunctionType(v) == FunctionType.Input){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.Input(v.getID());
        }else if ( getFunctionType(v) == FunctionType.Output){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.Output(v.getID());
        }else if ( getFunctionType(v) == FunctionType.HIGH){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.HIGH();
        }else if ( getFunctionType(v) == FunctionType.LOW){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.LOW();
        }else if ( getFunctionType(v) == FunctionType.D_LATCH){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.D_LATCH();
        }else if ( getFunctionType(v) == FunctionType.D_FLIP_FLOP){
            return Circuit.TEST? TestLogicGates.IO() : LogicGates.D_FLIP_FLOP();
        }
        else throw new MHDLException("NO SUCH GATE AVAILABLE");
    }


    public void verify(World worldIn, BlockPos pos) {

    }
}
