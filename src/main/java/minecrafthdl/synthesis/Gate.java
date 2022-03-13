package minecrafthdl.synthesis;

/**
 * Created by Francis O'Brien - 3/3/2017 - 9:47 AM
 */

public class Gate extends Circuit {

    public int num_inputs, num_outputs, input_spacing, output_spacing = 0;
    public int[] output_lines;

    public String[] id_txt;
    public boolean is_io = false;

    public Gate(int sizeX, int sizeY, int sizeZ, int num_inputs, int num_outputs, int input_spacing, int output_spacing, int[] output_lines) {
        super(sizeX, sizeY, sizeZ);

        this.num_inputs = num_inputs;
        this.num_outputs = num_outputs;
        this.input_spacing = input_spacing;
        this.output_spacing = output_spacing;
        this.output_lines = output_lines;
    }
}
