package dev.danipraivet;

import dev.danipraivet.modelo.datos.ConfiguracionBD;
import dev.danipraivet.modelo.seguridad.HashContrasena;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DiagnosticoLogin {

    private static final String USERNAME   = "jrodriguez";
    private static final String CONTRASENA = "Emp1234!";

    public static void main(String[] args) {

        try (Connection con = DriverManager.getConnection(
                ConfiguracionBD.URL, ConfiguracionBD.ADMIN_USER, ConfiguracionBD.ADMIN_PASS)) {

            System.out.println("Conexion OK con admin_app");

            PreparedStatement ps = con.prepareStatement(
                    "SELECT password_hash, activo, bloqueado, intentos_fallidos " +
                            "FROM empleados WHERE username = ?"
            );
            ps.setString(1, USERNAME);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("ERROR: usuario '" + USERNAME + "' no existe");
                return;
            }

            String hashEnBD  = rs.getString("password_hash");
            boolean bloqueado = rs.getBoolean("bloqueado");
            int intentos      = rs.getInt("intentos_fallidos");

            System.out.println("Hash en BD: " + hashEnBD.substring(0, Math.min(30, hashEnBD.length())) + "...");
            System.out.println("Bloqueado: " + bloqueado + "  Intentos: " + intentos);

            boolean esPlaceholder = hashEnBD.contains("examplehash") || hashEnBD.length() < 50;

            if (esPlaceholder) {
                System.out.println("Placeholder detectado, aplicando hashes reales...");
                aplicarTodosLosHashes(con);
            } else {
                boolean ok = HashContrasena.verificar(CONTRASENA, hashEnBD);
                System.out.println("BCrypt coincide para '" + CONTRASENA + "': " + (ok ? "SI" : "NO"));
                if (!ok) {
                    System.out.println("Regenerando y actualizando");
                    aplicarTodosLosHashes(con);
                } else if (bloqueado || intentos > 0) {
                    PreparedStatement fix = con.prepareStatement(
                            "UPDATE empleados SET bloqueado=FALSE, intentos_fallidos=0"
                    );
                    fix.executeUpdate();
                    System.out.println("Todos los empleados desbloqueados");
                } else {
                    System.out.println("Todo correcto, reinicia la app");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void aplicarTodosLosHashes(Connection con) throws Exception {
        int[]    codigos   = {1001,         1002,        1003,       1004,       1005};
        String[] passwords = {"Admin1234!", "RRHH1234!", "Emp1234!", "Emp1234!", "Emp1234!"};

        PreparedStatement ps = con.prepareStatement(
                "UPDATE empleados SET password_hash=?, bloqueado=FALSE, intentos_fallidos=0 WHERE cod_empleado=?"
        );

        int total = 0;
        for (int i = 0; i < codigos.length; i++) {
            String hash = HashContrasena.hashear(passwords[i]);
            ps.setString(1, hash);
            ps.setInt(2, codigos[i]);
            if (ps.executeUpdate() > 0) {
                System.out.printf("cod %d actualizado: %s...%n", codigos[i], hash.substring(0, 20));
                total++;
            }
        }

        System.out.println(total + "/5 empleados actualizados");
        System.out.println("cgarcia     Admin1234!");
        System.out.println("mmartinez   RRHH1234!");
        System.out.println("jrodriguez  Emp1234!");
        System.out.println("alopez      Emp1234!");
        System.out.println("pfernandez  Emp1234!");
    }
}