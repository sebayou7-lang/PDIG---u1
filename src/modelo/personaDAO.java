package modelo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class personaDAO {

    private File archivo;
    private persona persona;

    public personaDAO() {
        prepararArchivo();
    }

    public personaDAO(persona persona) {
        this.persona = persona;
        prepararArchivo();
    }

    // Método que prepara el archivo y directorio para almacenamiento
    private void prepararArchivo() {
        String directorioPath = "gestionContactos"; // Usando una ruta relativa
        File directorio = new File(directorioPath);
        if (!directorio.exists() && !directorio.mkdirs()) {
            System.err.println("Error al crear el directorio: " + directorioPath);
        }
        archivo = new File(directorio, "datosContactos.csv");
        if (!archivo.exists()) {
            try {
                if (archivo.createNewFile()) {
                    // Escribir encabezado
                    escribirLinea("NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO", false);
                } else {
                    System.err.println("No se pudo crear el archivo.");
                }
            } catch (IOException e) {
                System.err.println("Error al crear el archivo: " + e.getMessage());
            }
        }
    }

    // Método para escribir una línea en el archivo
    private void escribirLinea(String texto, boolean append) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo, append))) {
            bw.write(texto);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo: " + e.getMessage());
        }
    }

    // Método para escribir el contacto actual en el archivo
    public boolean escribirArchivo() {
        if (persona != null) {
            escribirLinea(persona.datosContacto(), true);
            return true;
        }
        return false;
    }

    // Método para leer los contactos del archivo
    public List<persona> leerArchivo() throws IOException {
        List<persona> personas = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean esEncabezado = true;

            while ((linea = br.readLine()) != null) {
                if (esEncabezado) {
                    esEncabezado = false;
                    continue;
                }
                String[] datos = linea.split(";");
                if (datos.length >= 5) {
                    persona p = new persona();
                    p.setNombre(datos[0]);
                    p.setTelefono(datos[1]);
                    p.setEmail(datos[2]);
                    p.setCategoria(datos[3]);
                    p.setFavorito(Boolean.parseBoolean(datos[4]));
                    personas.add(p);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            throw e;  // Volver a lanzar la excepción después de loguearla
        }
        return personas;
    }

    // Método para actualizar los contactos en el archivo
    public void actualizarContactos(List<persona> personas) throws IOException {
        if (archivo.exists()) {
            if (!archivo.delete()) {
                System.err.println("No se pudo eliminar el archivo antiguo.");
                return;
            }
        }
        if (!archivo.createNewFile()) {
            System.err.println("No se pudo crear el archivo.");
            return;
        }
        escribirLinea("NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO", true);
        for (persona p : personas) {
            escribirLinea(p.datosContacto(), true);
        }
    }

    // Función para eliminar contacto por nombre
    public boolean eliminarContactoPorNombre(String nombre) throws IOException {
        // Leer todos los contactos actuales
        List<persona> personas = leerArchivo();

        // Filtrar la lista para eliminar el contacto que coincida con el nombre
        boolean eliminado = personas.removeIf(p -> p.getNombre().equalsIgnoreCase(nombre));

        if (eliminado) {
            // Si se eliminó al menos un contacto, actualizamos el archivo
            actualizarContactos(personas);
            return true;  // Indicamos que el contacto fue eliminado
        }

        return false;  // Si no se encontró un contacto con el nombre especificado
    }
}
