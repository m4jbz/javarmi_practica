-- ===========================================================================
--  Datos de prueba de la agenda personal (76 contactos).
--
--  Solo se insertan las tres columnas obligatorias; las fechas las genera
--  la propia base de datos.
--
--  Los nombres usan acentos y enie (el Validador los acepta) y los telefonos
--  combinan ladas de Guerrero (733 Iguala, 744 Acapulco, 747 Chilpancingo,
--  762 Taxco, 736 Teloloapan, 767 Altamirano) con otras ciudades del pais.
--  La base guarda UTF-8, asi que el archivo debe leerse en UTF-8.
--
--  Ejecucion:
--      sqlite3 db/agenda.db < db/datos_prueba.sql
--
--  Los correos son distintos entre si porque la columna email es UNIQUE:
--  volver a ejecutar el script sobre los mismos datos provocara, de manera
--  intencional, el error de clave duplicada.
-- ===========================================================================

INSERT INTO Contactos (nombre, telefono, email) VALUES
    ('Cesar Saidt Corona Najera',           '7331234567', 'cesar.corona@itiguala.edu.mx'),
    ('Maria del Carmen Uriostegui Peralta', '7337654321', 'carmen.uriostegui@itiguala.edu.mx'),
    ('Ana Laura Mendoza Rios',              '7335551122', 'ana.mendoza@itiguala.edu.mx'),
    ('Jorge Alberto Ramirez Solis',         '7339988776', 'jorge.ramirez@itiguala.edu.mx'),
    ('Luis Fernando Castro Vega',           '7334455667', 'luis.castro@itiguala.edu.mx'),
    ('Sofia Guadalupe Torres Lopez',        '7332233445', 'sofia.torres@itiguala.edu.mx'),
    ('Mónica Pérez Espinoza',               '7478757491', 'monica_perez@gmail.com'),
    ('Paola Flores Soto',                   '7472760189', 'pflores53@outlook.com'),
    ('Hugo Guzmán Villalobos',              '5571147104', 'hugo_guzman@telmex.com'),
    ('Santiago Moreno Pineda',              '7625075291', 'santiagomp@icloud.com'),
    ('Regina Toledo Moreno',                '7333667127', 'rtoledo80@outlook.com'),
    ('Leonardo Ramírez Astudillo',          '5546563212', 'leonardora@gmail.com'),
    ('Ulises Hernández Villalobos',         '5524402685', 'uhernandez82@guerrero.gob.mx'),
    ('Lucía Ramírez Orozco',                '5590786666', 'lucia_ramirez@gmail.com'),
    ('Ximena López Morales',                '7333721590', 'lopez.ximena@gmail.com'),
    ('Julián Cruz Carbajal',                '7335901396', 'jcruz91@gmail.com'),
    ('Raúl Ocampo Guzmán',                  '7477117777', 'raulog@hotmail.com'),
    ('Daniela Rodríguez Rosales',           '7474728038', 'rodriguez.daniela@outlook.com'),
    ('Diego García Soto',                   '5541485253', 'garcia.diego@itiguala.edu.mx'),
    ('Renata Bahena Juárez',                '7449336338', 'renata.bahena@icloud.com'),
    ('Karla García Uribe',                  '7447439575', 'karlagu@outlook.com'),
    ('Arturo Rodríguez Jiménez',            '7773537990', 'arodriguez93@icloud.com'),
    ('Emiliano Zamora Lara',                '7331637265', 'emiliano_zamora@gmail.com'),
    ('Andrea Figueroa Rosales',             '7332220297', 'figueroa.andrea@telmex.com'),
    ('Marco Antonio Guzmán Estrada',        '7225288200', 'marco.guzman@softtek.com'),
    ('Montserrat Bello Rosales',            '7336330434', 'bello.montserrat@live.com.mx'),
    ('Jimena Salgado Díaz',                 '5562057986', 'salgado.jimena@live.com.mx'),
    ('Elena Cruz Bello',                    '5507290222', 'elena.cruz@icloud.com'),
    ('Gerardo Delgado López',               '7478887180', 'gdelgado34@hotmail.com'),
    ('Camila Martínez Toledo',              '7338780175', 'martinez.camila@guerrero.gob.mx'),
    ('Iker Aguirre Morales',                '7364788783', 'iaguirre76@cemex.com'),
    ('Rubén Delgado Morales',               '7772616751', 'ruben_delgado@bimbo.com.mx'),
    ('Tomás González Vázquez',              '7224125242', 'tomas.gonzalez@yahoo.com.mx'),
    ('Rafael Figueroa Villalobos',          '7333268656', 'rfigueroa55@hotmail.com'),
    ('Víctor Manuel Pérez Quintero',        '7470587706', 'perez.victor@outlook.com'),
    ('Beatriz Moreno Aguirre',              '7331311440', 'bmoreno33@uagro.mx'),
    ('Salvador Soto Ramírez',               '7624628897', 'salvador.soto@cemex.com'),
    ('Guadalupe Ortiz López',               '7362614014', 'guadalupeol@gmail.com'),
    ('Abril González Díaz',                 '7337058649', 'gonzalez.abril@gmail.com'),
    ('Araceli Pineda Reyes',                '7332402344', 'apineda36@live.com.mx'),
    ('Liliana Adame Aguirre',               '7222450400', 'adame.liliana@gmail.com'),
    ('Mateo Morales Aguirre',               '7773716786', 'mateoma@live.com.mx'),
    ('Joaquín Jiménez Bahena',              '7442650201', 'joaquin_jimenez@telmex.com'),
    ('Alan Flores López',                   '7336849340', 'alanfl@yahoo.com.mx'),
    ('Dulce María Ortiz Adame',             '7334558530', 'dortiz37@hotmail.com'),
    ('Mariana Gómez Hernández',             '7476174833', 'mariana.gomez@live.com.mx'),
    ('Alondra Díaz Pérez',                  '7336906044', 'alondra.diaz@telmex.com'),
    ('Adriana Fuentes Bello',               '7339657249', 'adriana.fuentes@telmex.com'),
    ('Natalia Pineda Aguirre',              '8168288909', 'nataliapa@cemex.com'),
    ('Francisco Javier Pérez García',       '7332516780', 'perez.francisco@telmex.com'),
    ('Brenda Núñez Reyes',                  '7774071881', 'brenda.nunez@bimbo.com.mx'),
    ('Rocío Rosales Estrada',               '7441433377', 'rocio_rosales@outlook.com'),
    ('Yesenia Núñez Moreno',                '7339319254', 'ynunez98@telmex.com'),
    ('Miguel Ángel Ibarra Espinoza',        '7330707413', 'mibarra72@bimbo.com.mx'),
    ('Ricardo Pineda Bello',                '7477771834', 'ricardo.pineda@gmail.com'),
    ('Héctor Moreno Román',                 '7338746331', 'hectormr@itiguala.edu.mx'),
    ('Samuel Rosales Bello',                '7445298415', 'samuel_rosales@hotmail.com'),
    ('Fernanda Figueroa García',            '7330776426', 'ffigueroa58@outlook.com'),
    ('Itzel Sánchez Bahena',                '7335561304', 'itzel.sanchez@hotmail.com'),
    ('Frida Figueroa Jaimes',               '5515640410', 'fridafj@bimbo.com.mx'),
    ('Valeria Reyes Ortiz',                 '7628535606', 'valeriaro@itiguala.edu.mx'),
    ('Nayeli Quintero Pérez',               '7336792470', 'nayeliqp@itiguala.edu.mx'),
    ('Viridiana Estrada Catalán',           '7474444634', 'viridiana_estrada@icloud.com'),
    ('Perla Sánchez Flores',                '8121387837', 'perla_sanchez@outlook.com'),
    ('Sebastián Ramírez Delgado',           '7443125815', 'sramirez57@hotmail.com'),
    ('Eduardo Zamora Espinoza',             '7440666836', 'eduardo.zamora@hotmail.com'),
    ('Noé Villalobos Ortiz',                '5552883143', 'noe_villalobos@outlook.com'),
    ('Óscar Astudillo Gutiérrez',           '7332067970', 'astudillo.oscar@gmail.com'),
    ('Armando Román Adame',                 '7441322817', 'armando.roman@gmail.com'),
    ('Citlali Hernández Uribe',             '7333904248', 'citlali.hernandez@telmex.com'),
    ('Gustavo Rodríguez González',          '7478936439', 'rodriguez.gustavo@gmail.com'),
    ('Rodrigo Gutiérrez Román',             '7445378383', 'rgutierrez62@gmail.com'),
    ('Estefanía López García',              '7447614365', 'estefania.lopez@hotmail.com'),
    ('Alejandro Orozco Bahena',             '7366563048', 'alejandro_orozco@gmail.com'),
    ('Iván Morales Gutiérrez',              '7443734419', 'ivanmg@icloud.com'),
    ('Ernesto Jiménez Villalobos',          '7620926030', 'ernesto_jimenez@guerrero.gob.mx');

-- Comprobacion rapida del contenido cargado.
SELECT COUNT(*) AS total FROM Contactos;
