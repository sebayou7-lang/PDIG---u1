package controlador;

import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

import vista.ventana;
import modelo.*;

public class logica_ventana implements ActionListener, ListSelectionListener, ItemListener {
	private ventana delegado;
	private String nombres, email, telefono, categoria = "";
	private persona persona;
	private List<persona> contactos;
	private boolean favorito = false;

	public logica_ventana(ventana delegado) {
		this.delegado = delegado;
		cargarContactosRegistrados();

		this.delegado.btn_add.addActionListener(this);
		this.delegado.btn_eliminar.addActionListener(this);
		this.delegado.btn_modificar.addActionListener(this);
		this.delegado.cmb_categoria.addItemListener(this);
		this.delegado.chb_favorito.addItemListener(this);
		this.delegado.btn_exportar.addActionListener(e -> exportarCSV());


		// Filtro en tiempo real
		this.delegado.txt_buscar.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				filtrarContactos(delegado.txt_buscar.getText());
			}
			public void removeUpdate(DocumentEvent e) {
				filtrarContactos(delegado.txt_buscar.getText());
			}
			public void changedUpdate(DocumentEvent e) {
				filtrarContactos(delegado.txt_buscar.getText());
			}
		});

		// Ctrl + S agrega contacto
		delegado.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke("control S"), "agregar");
		delegado.contentPane.getActionMap().put("agregar", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				delegado.btn_add.doClick();
			}
		});

		// Menú contextual para eliminar
		JPopupMenu menuContextual = new JPopupMenu();
		JMenuItem itemEliminar = new JMenuItem("Eliminar");
		menuContextual.add(itemEliminar);

		itemEliminar.addActionListener(e -> {
			int fila = delegado.tbl_contactos.getSelectedRow();
			if (fila != -1) {
				contactos.remove(fila);
				try {
					new personaDAO().actualizarContactos(contactos);
					limpiarCampos();
					JOptionPane.showMessageDialog(delegado, "Contacto eliminado correctamente");
				} catch (IOException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(delegado, "Error al eliminar el contacto.");
				}
			}
		});

		delegado.tbl_contactos.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) mostrarMenu(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) mostrarMenu(e);
			}
			private void mostrarMenu(MouseEvent e) {
				int fila = delegado.tbl_contactos.rowAtPoint(e.getPoint());
				if (fila != -1) {
					delegado.tbl_contactos.setRowSelectionInterval(fila, fila);
					menuContextual.show(delegado.tbl_contactos, e.getX(), e.getY());
				}
			}
		});
	}

	private void filtrarContactos(String texto) {
		DefaultTableModel modelo = delegado.modeloTabla;
		modelo.setRowCount(0);
		List<persona> filtrados = contactos.stream()
			.filter(p -> p.getNombre().toLowerCase().contains(texto.toLowerCase()))
			.collect(Collectors.toList());
		for (persona contacto : filtrados) {
			Object[] fila = {
					contacto.getNombre(),
					contacto.getTelefono(),
					contacto.getEmail(),
					contacto.getCategoria(),
					contacto.isFavorito() ? "Sí" : "No"
			};
			modelo.addRow(fila);
		}
	}

	private void incializacionCampos() {
		nombres = delegado.txt_nombres.getText();
		email = delegado.txt_email.getText();
		telefono = delegado.txt_telefono.getText();
	}

	private void cargarContactosRegistrados() {
		try {
			contactos = new personaDAO(new persona()).leerArchivo();
			DefaultTableModel modelo = delegado.modeloTabla;
			modelo.setRowCount(0);
			for (persona contacto : contactos) {
				Object[] fila = {
						contacto.getNombre(),
						contacto.getTelefono(),
						contacto.getEmail(),
						contacto.getCategoria(),
						contacto.isFavorito() ? "Sí" : "No"
				};
				modelo.addRow(fila);
			}
			actualizarEstadisticas();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(delegado, "Existen problemas al cargar todos los contactos");
		}
	}

	private void actualizarEstadisticas() {
		int total = contactos.size();
		int favoritos = (int) contactos.stream().filter(p -> p.isFavorito()).count();
		long familia = contactos.stream().filter(p -> p.getCategoria().equals("Familia")).count();
		long amigos = contactos.stream().filter(p -> p.getCategoria().equals("Amigos")).count();
		long trabajo = contactos.stream().filter(p -> p.getCategoria().equals("Trabajo")).count();

		delegado.lbl_totalContactos.setText("Total de contactos: " + total);
		delegado.lbl_favoritos.setText("Favoritos: " + favoritos);
		delegado.lbl_familia.setText("Familia: " + familia);
		delegado.lbl_amigos.setText("Amigos: " + amigos);
		delegado.lbl_trabajo.setText("Trabajo: " + trabajo);
	}

	private void limpiarCampos() {
		delegado.txt_nombres.setText("");
		delegado.txt_telefono.setText("");
		delegado.txt_email.setText("");
		categoria = "";
		favorito = false;
		delegado.chb_favorito.setSelected(favorito);
		delegado.cmb_categoria.setSelectedIndex(0);
		incializacionCampos();
		cargarContactosRegistrados();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		incializacionCampos();
		if (e.getSource() == delegado.btn_add) {
		    if (!nombres.equals("") && !telefono.equals("") && !email.equals("")) {
		        if (!categoria.equals("Elija una Categoria") && !categoria.equals("")) {
		            // Activar barra de progreso 
		            delegado.barraProgreso.setValue(0);
		            Timer timer = new Timer(10, null);
		            timer.addActionListener(new ActionListener() {
		                int progreso = 0;
		                @Override
		                public void actionPerformed(ActionEvent evt) {
		                    progreso++;
		                    delegado.barraProgreso.setValue(progreso);
		                    if (progreso >= 100) {
		                        timer.stop();
		                        persona persona = new persona(nombres, telefono, email, categoria, favorito);
		                        new personaDAO(persona).escribirArchivo();
		                        limpiarCampos();
		                        JOptionPane.showMessageDialog(delegado, "Contacto Registrado!!!");
		                        delegado.barraProgreso.setValue(0);
		                    }
		                }
		            });
		            timer.start();
		        } else {
		            JOptionPane.showMessageDialog(delegado, "Elija una Categoria!!!");
		        }
		    } else {
		        JOptionPane.showMessageDialog(delegado, "Todos los campos deben ser llenados!!!");
		    }
		}

		
		else if (e.getSource() == delegado.btn_modificar) {
			int fila = delegado.tbl_contactos.getSelectedRow();
			if (fila != -1) {
				incializacionCampos();
				if (!nombres.equals("") && !telefono.equals("") && !email.equals("")
						&& !categoria.equals("") && !categoria.equals("Elija una Categoria")) {

					persona actual = contactos.get(fila);
					boolean hayCambios =
							!actual.getNombre().equals(nombres) ||
							!actual.getTelefono().equals(telefono) ||
							!actual.getEmail().equals(email) ||
							!actual.getCategoria().equals(categoria) ||
							(actual.isFavorito() != favorito);

					if (!hayCambios) {
						JOptionPane.showMessageDialog(delegado, "Debes realizar algún cambio antes de modificar el contacto.");
						return;
					}

					persona nuevo = new persona(nombres, telefono, email, categoria, favorito);
					contactos.set(fila, nuevo);
					try {
						new personaDAO().actualizarContactos(contactos);
						limpiarCampos();
						JOptionPane.showMessageDialog(delegado, "Contacto modificado correctamente");
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(delegado, "Error al modificar el contacto.");
					}
				} else {
					JOptionPane.showMessageDialog(delegado, "Todos los campos deben estar completos para modificar");
				}
			}
		} else if (e.getSource() == delegado.btn_eliminar) {
			int fila = delegado.tbl_contactos.getSelectedRow();
			if (fila != -1) {
				contactos.remove(fila);
				try {
					new personaDAO().actualizarContactos(contactos);
					limpiarCampos();
					JOptionPane.showMessageDialog(delegado, "Contacto eliminado correctamente");
				} catch (IOException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(delegado, "Error al eliminar el contacto.");
				}
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// No se usa directamente con JTable
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == delegado.cmb_categoria) {
			categoria = delegado.cmb_categoria.getSelectedItem().toString();
		} else if (e.getSource() == delegado.chb_favorito) {
			favorito = delegado.chb_favorito.isSelected();
		}
	}
	private void exportarCSV() {
	    JFileChooser chooser = new JFileChooser();
	    chooser.setDialogTitle("Guardar contactos como CSV");
	    chooser.setSelectedFile(new java.io.File(System.getProperty("user.home") + "/Downloads/contactos.csv"));
	    
	    int userSelection = chooser.showSaveDialog(delegado);
	    
	    if (userSelection == JFileChooser.APPROVE_OPTION) {
	        java.io.File archivo = chooser.getSelectedFile();
	        try (java.io.FileWriter writer = new java.io.FileWriter(archivo)) {
	            writer.write("NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO\n");
	            for (persona p : contactos) {
	                writer.write(p.getNombre() + ";" +
	                             p.getTelefono() + ";" +
	                             p.getEmail() + ";" +
	                             p.getCategoria() + ";" +
	                             p.isFavorito() + "\n");
	            }
	            JOptionPane.showMessageDialog(delegado, "Contactos exportados correctamente.");
	        } catch (IOException ex) {
	            ex.printStackTrace();
	            JOptionPane.showMessageDialog(delegado, "Error al exportar el archivo.");
	        }
	    }
	}

}

