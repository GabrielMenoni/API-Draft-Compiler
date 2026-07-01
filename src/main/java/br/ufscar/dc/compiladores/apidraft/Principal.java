package br.ufscar.dc.compiladores.apidraft;

public class Principal {
    private static final String USO = "Uso: java -jar api-draft-compiler.jar --target <kotlin|typescript> --output <dir> <arquivo.apid>";

    public static void main(String[] args) {
        if (!argumentosValidos(args)) {
            System.out.println(USO);
            System.exit(1);
        }

        String target = args[1];
        String outputDir = args[3];
        String inputFile = args[4];

        System.out.println("Compilando: " + inputFile + " -> " + target + " em " + outputDir);
    }

    private static boolean argumentosValidos(String[] args) {
        if (args.length != 5) return false;
        if (!args[0].equals("--target")) return false;
        if (!args[1].equals("kotlin") && !args[1].equals("typescript")) return false;
        if (!args[2].equals("--output")) return false;
        return true;
    }
}
