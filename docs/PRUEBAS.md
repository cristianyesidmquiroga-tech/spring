# Pruebas: equivalencia con Portería 2

Este documento relaciona una a una las funciones pytest de Portería 2 (Flask) con las pruebas JUnit de este repositorio.
Para correrlas: `./mvnw verify` (necesita Docker, porque la base de pruebas es un PostgreSQL en Testcontainers).
El informe de cobertura queda en `target/site/jacoco/index.html`.
GitHub Actions las corre en cada push con `.github/workflows/pruebas.yml`.

## Herramientas

| Portería 2 (Python) | Este repositorio (Java) |
|---|---|
| pytest | JUnit 5 |
| `assert` | AssertJ (`assertThat`) |
| `@pytest.mark.parametrize` | `@ParameterizedTest` con `@ValueSource`, `@CsvSource`, `@EnumSource` o `@MethodSource` |
| fixtures y `conftest.py` | `soporte/PruebaIntegracion` y `roles/PruebaRol` |
| `client` de Flask | MockMvc |
| `monkeypatch` | Mockito |
| base SQLite / PostgreSQL | Testcontainers con PostgreSQL 16 |

## Carpetas

Las carpetas de `tests/modulos` y `tests/roles/<perfil>` se llaman igual en Java (`modulos`, `roles/<perfil>`).
En `tests/vistas` los nombres compuestos pasan a una sola palabra:

| pytest `tests/vistas/<carpeta>` | Java `vistas/<carpeta>` |
|---|---|
| cambio_contrasena | contrasena |
| cerrar_sesion | sesion |
| gestion_usuarios | usuarios |
| historial_ingresos | ingresos |
| revision_fotos | fotos |
| bandeja_mensajes | bandeja |
| historial_cambios | auditoria |
| historial_clases | clases |
| politica_privacidad | privacidad |

Las demás (carnet, equipos, escaner, login, panel, pases, perfil, reportes, y las que aún no existen como ambientes o fichas) conservan su nombre.

## Estados

- **Migrada**: existe el método Java y pasa.
- **Programada fase N**: la función todavía no existe en Spring. Fase 5, cuentas y operación: registro, verificación de correo, recuperación, historial de cambios, respaldos, importación de Excel, captcha y política de privacidad.
- **Descartada**: no se migra por una decisión explícita; debajo de la tabla del archivo van el motivo y la fecha.

Cada clase Java empieza con el comentario `// Portería 2: tests/<ruta>.py`, que es el archivo que se lista en cada sección.

## Equivalencia por archivo

### tests/modulos/test_acceso_fotos.py

Java: `modulos.AccesoFotosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_la_carpeta_publica_ya_no_sirve_fotos` | `AccesoFotosTest#laCarpetaPublicaYaNoSirveFotos` | Migrada |
| `test_la_vista_de_fotos_exige_sesion` | `AccesoFotosTest#laVistaDeFotosExigeSesion` | Migrada |
| `test_no_confirma_si_la_persona_existe` | `AccesoFotosTest#noConfirmaSiLaPersonaExiste` | Migrada |
| `test_cada_quien_ve_la_suya` | `AccesoFotosTest#cadaQuienVeLaSuya` | Migrada |
| `test_un_aprendiz_no_ve_la_de_otro` | `AccesoFotosTest#unAprendizNoVeLaDeOtro` | Migrada |
| `test_nadie_sin_autenticar` | `AccesoFotosTest#nadieSinAutenticar` | Migrada |
| `test_uno_mismo_si` | `AccesoFotosTest#unoMismoSi` | Migrada |
| `test_un_aprendiz_no_ve_la_de_otro` | `AccesoFotosTest#reglaUnAprendizNoVeLaDeOtro` | Migrada |
| `test_quien_la_necesita_por_su_funcion_si` (casos: es_admin, puede_operar_porteria, puede_asesorar, puede_gestionar_asistencia) | `AccesoFotosTest#quienLaNecesitaPorSuFuncionSi` | Migrada |

### tests/modulos/test_almacenamiento.py

Java: `modulos.AlmacenamientoTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_permitidas` (casos: foto.jpg, FOTO.JPEG, a.png, b.webp) | `AlmacenamientoTest#permitidas` | Migrada |
| `test_rechazadas` (casos: x.svg, x.php, x.exe, sin_extension, vacío, None) | `AlmacenamientoTest#rechazadas` | Migrada |
| `test_nombre_fijo_por_usuario` | `AlmacenamientoTest#nombreFijoPorUsuario` | Migrada |
| `test_reescala_comprime_y_convierte` | `AlmacenamientoTest#reescalaComprimeYConvierte` | Migrada |
| `test_elimina_los_metadatos_exif` | `AlmacenamientoTest#eliminaLosMetadatosExif` | Migrada |
| `test_aplana_la_transparencia_sobre_blanco` | `AlmacenamientoTest#aplanaLaTransparenciaSobreBlanco` | Migrada |
| `test_rechaza_lo_que_no_es_una_imagen` | `AlmacenamientoTest#rechazaLoQueNoEsUnaImagen` | Migrada |
| `test_crea_la_carpeta_si_no_existe` | `AlmacenamientoTest#creaLaCarpetaSiNoExiste` | Migrada |
| `test_acepta_un_retrato_real` | — | Descartada |
| `test_acepta_a_quien_lleva_gafas` | — | Descartada |
| `test_acepta_el_retrato_en_distintas_condiciones` | — | Descartada |
| `test_rechaza_fotos_con_mas_de_una_persona` | — | Descartada |
| `test_rechaza_un_rostro_diminuto` | — | Descartada |
| `test_rechaza_imagenes_sin_rostro` (casos: 3 colores) | — | Descartada |
| `test_rechaza_dibujos_y_capturas` | — | Descartada |
| `test_no_bloquea_el_sistema_si_falta_el_modelo` | — | Descartada |
| `test_la_foto_se_guarda_comprimida_y_con_nombre_fijo` | `AlmacenamientoTest#laFotoSeGuardaComprimidaYConNombreFijo` | Migrada |
| `test_la_subida_funciona_con_las_fotos_en_otro_volumen` | `AlmacenamientoTest#laSubidaFuncionaConLasFotosEnOtroVolumen` | Migrada |
| `test_un_archivo_invalido_no_borra_la_foto_anterior` | `AlmacenamientoTest#unArchivoInvalidoNoBorraLaFotoAnterior` | Migrada |
| `test_no_mezcla_accesos_de_distintas_entidades` | — | Programada fase 5 |
| `test_solo_borra_si_se_pide_a_proposito` | — | Programada fase 5 |
| `test_rechaza_el_rostro_tapado` (casos: mascarilla azul, mascarilla negra, mascarilla color piel, bufanda, gorra calada) | — | Descartada |
| `test_las_gafas_siguen_aceptandose` | — | Descartada |

Descartada (TestValidacionFacial y TestRostroTapado): Cristian decidió el 2026-09-24 no usar OpenCV; la foto la revisa a mano el administrador.

### tests/modulos/test_autenticacion.py

Java: `modulos.AutenticacionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_credenciales_correctas` | `AutenticacionTest#credencialesCorrectas` | Migrada |
| `test_no_revela_si_la_cuenta_existe` | `AutenticacionTest#noRevelaSiLaCuentaExiste` | Migrada |
| `test_bloquea_tras_cinco_intentos` | `AutenticacionTest#bloqueaTrasCincoIntentos` | Migrada |
| `test_el_contador_se_reinicia_al_acertar` | `AutenticacionTest#elContadorSeReiniciaAlAcertar` | Migrada |
| `test_login_sin_contrasena_no_entra` | `AutenticacionTest#loginSinContrasenaNoEntra` | Migrada |
| `test_respuesta_identica_exista_o_no_la_cuenta` | — | Programada fase 5 |
| `test_codigo_incorrecto_se_anula_tras_cinco_intentos` | — | Programada fase 5 |
| `test_pedir_codigo_no_reinicia_el_bloqueo_del_login` | — | Programada fase 5 |
| `test_no_se_puede_saltar_al_paso_de_cambio` | — | Programada fase 5 |
| `test_codigo_expirado_se_rechaza` | — | Programada fase 5 |
| `test_registro_valido_crea_la_cuenta` | — | Programada fase 5 |
| `test_sin_autorizacion_de_datos_no_se_registra` | — | Programada fase 5 |
| `test_contrasena_vacia_rechazada` | — | Programada fase 5 |
| `test_contrasena_corta_rechazada` | — | Programada fase 5 |
| `test_restriccion_de_dominio` | — | Programada fase 5 |

### tests/modulos/test_avatares.py

Java: `modulos.AvataresTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cada_cargo_tiene_el_suyo` (casos: Aprendiz, Instructor, Celador, Administrador, Administrativo) | `AvataresTest#cadaCargoTieneElSuyo` | Migrada |
| `test_no_distingue_mayusculas_ni_espacios` | `AvataresTest#noDistingueMayusculasNiEspacios` | Migrada |
| `test_porteria_comparte_avatar_con_celador` | `AvataresTest#porteriaComparteAvatarConCelador` | Migrada |
| `test_cargo_desconocido_cae_al_generico` (casos: None, vacío, Cargo Inventado, xyz) | `AvataresTest#cargoDesconocidoCaeAlGenerico` | Migrada |
| `test_todos_los_archivos_existen` | `AvataresTest#todosLosArchivosExisten` | Migrada |
| `test_los_svg_no_traen_scripts` | `AvataresTest#losSvgNoTraenScripts` | Migrada |
| `test_sin_foto_devuelve_el_avatar_del_cargo` | `AvataresTest#sinFotoDevuelveElAvatarDelCargo` | Migrada |
| `test_foto_inexistente_cae_al_avatar` | `AvataresTest#fotoInexistenteCaeAlAvatar` | Migrada |
| `test_foto_existente_se_usa` | `AvataresTest#fotoExistenteSeUsa` | Migrada |
| `test_ninguna_plantilla_llama_a_ui_avatars` | `AvataresTest#ningunaPlantillaLlamaAUiAvatars` | Migrada |
| `test_el_csp_no_permite_ui_avatars` | `AvataresTest#elCspNoPermiteUiAvatars` | Migrada |

### tests/modulos/test_cargos_validos.py

Java: `modulos.CargosValidosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_los_cuatro_cargos_estan_en_la_lista_blanca_y_en_el_carnet` | `CargosValidosTest#losCuatroCargosEstanEnLaListaBlancaYEnElCarnet` | Migrada |
| `test_el_panel_expone_la_lista_blanca` | `CargosValidosTest#elPanelExponeLaListaBlanca` | Migrada |
| `test_crear_y_editar_rechazan_cargos_fuera_de_la_lista` | `CargosValidosTest#crearYEditarRechazanCargosFueraDeLaLista` | Migrada |
| `test_registro_con_sesion_admin_tambien_valida_el_cargo` | — | Programada fase 5 |
| `test_los_cargos_nuevos_se_crean_con_sus_permisos` | `CargosValidosTest#losCargosNuevosSeCreanConSusPermisos` | Migrada |
| `test_importacion_acepta_los_cargos_nuevos_y_avisa_los_invalidos` | — | Programada fase 5 |

### tests/modulos/test_carnet.py

Java: `modulos.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cada_cargo_actual_tiene_perfil` (casos: Aprendiz, Instructor, Celador, Portería, Administrativo, Administrador) | `CarnetTest#cadaCargoActualTienePerfil` | Migrada |
| `test_un_cargo_desconocido_no_revienta` | `CarnetTest#unCargoDesconocidoNoRevienta` | Migrada |
| `test_no_distingue_mayusculas` | `CarnetTest#noDistingueMayusculas` | Migrada |
| `test_se_puede_reasignar_por_variable_de_entorno` | `CarnetTest#sePuedeReasignarPorVariableDeEntorno` | Migrada |
| `test_un_valor_mal_escrito_se_ignora` | `CarnetTest#unValorMalEscritoSeIgnora` | Migrada |
| `test_el_perfil_no_cambia_los_permisos` | `CarnetTest#elPerfilNoCambiaLosPermisos` | Migrada |
| `test_si_los_declaro_se_usan_tal_cual` | `CarnetTest#siLosDeclaroSeUsanTalCual` | Migrada |
| `test_cuatro_palabras_se_parten_por_la_mitad` | `CarnetTest#cuatroPalabrasSePartenPorLaMitad` | Migrada |
| `test_tres_palabras_van_uno_y_dos` | `CarnetTest#tresPalabrasVanUnoYDos` | Migrada |
| `test_dos_palabras` | `CarnetTest#dosPalabras` | Migrada |
| `test_una_sola_palabra_no_inventa_apellido` | `CarnetTest#unaSolaPalabraNoInventaApellido` | Migrada |
| `test_cinco_palabras_dejan_lo_sobrante_en_apellidos` | `CarnetTest#cincoPalabrasDejanLoSobranteEnApellidos` | Migrada |
| `test_vacio_no_revienta` | `CarnetTest#vacioNoRevienta` | Migrada |
| `test_los_campos_declarados_mandan_sobre_el_reparto` | `CarnetTest#losCamposDeclaradosMandanSobreElReparto` | Migrada |
| `test_abreviaturas` | `CarnetTest#abreviaturas` | Migrada |
| `test_el_carnet_muestra_tipo_y_numero` | `CarnetTest#elCarnetMuestraTipoYNumero` | Migrada |
| `test_sin_documento_no_inventa_nada` | `CarnetTest#sinDocumentoNoInventaNada` | Migrada |
| `test_la_tabla_de_patrones_es_valida` | `CarnetTest#laTablaDePatronesEsValida` | Migrada |
| `test_el_digito_de_control_es_el_esperado` | `CarnetTest#elDigitoDeControlEsElEsperado` | Migrada |
| `test_codifica_los_codigos_reales_del_sistema` (casos: 5 códigos de carnet y pases) | `CarnetTest#codificaLosCodigosRealesDelSistema` | Migrada |
| `test_el_svg_escala_al_ancho_del_contenedor` | `CarnetTest#elSvgEscalaAlAnchoDelContenedor` | Migrada |
| `test_un_caracter_no_representable_se_rechaza` | `CarnetTest#unCaracterNoRepresentableSeRechaza` | Migrada |
| `test_el_simbolo_crece_11_modulos_por_caracter` | `CarnetTest#elSimboloCrece11ModulosPorCaracter` | Migrada |
| `test_el_carnet_de_aprendiz_lleva_ficha_programa_fecha_y_poliza` | `CarnetTest#elCarnetDeAprendizLlevaFichaProgramaFechaYPoliza` | Migrada |
| `test_los_demas_perfiles_no_llevan_ficha_ni_poliza` (casos: Instructor, Celador, Administrativo) | `CarnetTest#losDemasPerfilesNoLlevanFichaNiPoliza` | Migrada |
| `test_todos_los_perfiles_llevan_codigo_de_barras` | `CarnetTest#todosLosPerfilesLlevanCodigoDeBarras` | Migrada |
| `test_sin_perfil_completo_no_hay_codigo` | `CarnetTest#sinPerfilCompletoNoHayCodigo` | Migrada |
| `test_no_quedan_escritas_a_fuego_en_las_plantillas` | `CarnetTest#noQuedanEscritasAFuegoEnLasPlantillas` | Migrada |
| `test_estan_disponibles_en_todas_las_plantillas` | `CarnetTest#estanDisponiblesEnTodasLasPlantillas` | Migrada |
| `test_cambiarlas_cambia_lo_que_se_imprime` | `CarnetTest.OtroCentro#cambiarlasCambiaLoQueSeImprime` | Migrada |

### tests/modulos/test_documentos.py

Java: `modulos.DocumentosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_quita_puntos_espacios_y_guiones` (casos: 4 formatos) | `DocumentosTest#quitaPuntosEspaciosYGuiones` | Migrada |
| `test_el_numero_se_guarda_normalizado` | `DocumentosTest#elNumeroSeGuardaNormalizado` | Migrada |
| `test_acepta_longitudes_validas` (casos: 9 pares tipo/número) | `DocumentosTest#aceptaLongitudesValidas` | Migrada |
| `test_rechaza_longitudes_invalidas` (casos: 5 pares tipo/número) | `DocumentosTest#rechazaLongitudesInvalidas` | Migrada |
| `test_el_mensaje_sugiere_revisar_el_tipo` | `DocumentosTest#elMensajeSugiereRevisarElTipo` | Migrada |
| `test_rechaza_letras_donde_solo_van_numeros` | `DocumentosTest#rechazaLetrasDondeSoloVanNumeros` | Migrada |
| `test_el_pasaporte_si_admite_letras` | `DocumentosTest#elPasaporteSiAdmiteLetras` | Migrada |
| `test_rechaza_que_empiece_por_cero` | `DocumentosTest#rechazaQueEmpiecePorCero` | Migrada |
| `test_rechaza_vacio` | `DocumentosTest#rechazaVacio` | Migrada |
| `test_rechaza_un_tipo_inventado` | `DocumentosTest#rechazaUnTipoInventado` | Migrada |
| `test_todos_los_tipos_describen_su_formato` | `DocumentosTest#todosLosTiposDescribenSuFormato` | Migrada |
| `test_sugiere_tarjeta_de_identidad_con_once_digitos` | `DocumentosTest#sugiereTarjetaDeIdentidadConOnceDigitos` | Migrada |
| `test_sugiere_pasaporte_si_tiene_letras` | `DocumentosTest#sugierePasaporteSiTieneLetras` | Migrada |
| `test_guardar_un_documento_valido` | `DocumentosTest#guardarUnDocumentoValido` | Migrada |
| `test_un_documento_invalido_se_rechaza` | `DocumentosTest#unDocumentoInvalidoSeRechaza` | Migrada |
| `test_no_se_puede_usar_el_documento_de_otro` | `DocumentosTest#noSePuedeUsarElDocumentoDeOtro` | Migrada |
| `test_registro_con_documento_valido` | — | Programada fase 5 |
| `test_registro_con_documento_invalido` | — | Programada fase 5 |
| `test_una_tarjeta_de_identidad_de_menor` | — | Programada fase 5 |

### tests/modulos/test_email.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_envio_correcto_devuelve_entregado` | — | Programada fase 5 |
| `test_credenciales_rechazadas_es_fallo_definitivo` | — | Programada fase 5 |
| `test_destinatario_inexistente_es_fallo_definitivo` | — | Programada fase 5 |
| `test_remitente_rechazado_es_fallo_definitivo` | — | Programada fase 5 |
| `test_rechazo_5xx_es_fallo_definitivo` | — | Programada fase 5 |
| `test_rechazo_4xx_es_fallo_pasajero` | — | Programada fase 5 |
| `test_corte_de_red_es_fallo_pasajero` | — | Programada fase 5 |
| `test_servidor_que_cuelga_es_fallo_pasajero` | — | Programada fase 5 |
| `test_fallo_definitivo_no_se_reintenta` | — | Programada fase 5 |
| `test_fallo_pasajero_si_se_reintenta` | — | Programada fase 5 |
| `test_no_se_reintenta_indefinidamente` | — | Programada fase 5 |
| `test_la_espera_del_reintento_no_bloquea_al_resto` | — | Programada fase 5 |
| `test_el_temporizador_del_reintento_es_demonio` | — | Programada fase 5 |
| `test_correos_pendientes_nunca_es_negativo_por_la_ruta_del_reintento` | — | Programada fase 5 |
| `test_correos_pendientes_cuenta_el_correo_en_vuelo` | — | Programada fase 5 |
| `test_cada_puerto_elige_su_cifrado` (casos: 465, 587, 25, 2525) | — | Programada fase 5 |
| `test_un_valor_valido_de_mail_cifrado_se_respeta` (casos: ssl, STARTTLS, ninguno) | — | Programada fase 5 |
| `test_mail_cifrado_invalido_no_desactiva_el_cifrado` (casos: 6 valores x 3 puertos) | — | Programada fase 5 |
| `test_puerto_465_abre_el_canal_ya_cifrado` | — | Programada fase 5 |
| `test_puerto_587_cifra_antes_de_autenticar` | — | Programada fase 5 |
| `test_un_mail_cifrado_invalido_no_manda_la_clave_en_claro` | — | Programada fase 5 |
| `test_las_credenciales_rechazadas_no_escriben_el_remitente_entero` | — | Programada fase 5 |
| `test_ninguna_direccion_completa_aparece_en_el_registro` (casos: envío correcto, credenciales rechazadas) | — | Programada fase 5 |
| `test_el_destinatario_rechazado_tampoco_se_escribe_entero` | — | Programada fase 5 |
| `test_ofuscar_no_revela_el_usuario_completo` | — | Programada fase 5 |
| `test_el_hilo_enviador_sobrevive_a_un_elemento_corrupto` | — | Programada fase 5 |
| `test_enviar_correo_encola_y_vuelve_pronto` | — | Programada fase 5 |
| `test_enviar_correo_rechaza_un_destinatario_invalido` | — | Programada fase 5 |
| `test_el_modo_directo_se_conserva` | — | Programada fase 5 |
| `test_el_modo_directo_cae_al_rele_si_falla` | — | Programada fase 5 |
| `test_el_modo_directo_sin_respaldo_es_pasajero` | — | Programada fase 5 |

### tests/modulos/test_escaneo_puerta.py

Java: `modulos.EscaneoPuertaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_documento_valido_devuelve_datos_del_usuario` | `EscaneoPuertaTest#documentoValidoDevuelveDatosDelUsuario` | Migrada |
| `test_documento_inexistente_no_se_encuentra` | `EscaneoPuertaTest#documentoInexistenteNoSeEncuentra` | Migrada |
| `test_documento_de_perfil_incompleto_se_puede_verificar` | `EscaneoPuertaTest#documentoDePerfilIncompletoSePuedeVerificar` | Migrada |
| `test_foto_no_aprobada_se_marca_explicitamente` | `EscaneoPuertaTest#fotoNoAprobadaSeMarcaExplicitamente` | Migrada |
| `test_foto_aprobada_se_marca_true` | `EscaneoPuertaTest#fotoAprobadaSeMarcaTrue` | Migrada |
| `test_documento_valido_muestra_perfil` | `EscaneoPuertaTest#documentoValidoMuestraPerfil` | Migrada |
| `test_documento_inexistente_redirige_con_aviso` | `EscaneoPuertaTest#documentoInexistenteRedirigeConAviso` | Migrada |
| `test_perfil_incompleto_se_puede_ver` | `EscaneoPuertaTest#perfilIncompletoSePuedeVer` | Migrada |
| `test_foto_no_aprobada_no_impide_ver_la_pagina` | `EscaneoPuertaTest#fotoNoAprobadaNoImpideVerLaPagina` | Migrada |
| `test_aprendiz_no_consulta_api_verify` | `EscaneoPuertaTest#aprendizNoConsultaApiVerify` | Migrada |
| `test_aprendiz_no_consulta_verify_pagina` | `EscaneoPuertaTest#aprendizNoConsultaVerifyPagina` | Migrada |
| `test_no_autenticado_no_consulta` | `EscaneoPuertaTest#noAutenticadoNoConsulta` | Migrada |
| `test_celador_registra_incidente` | `EscaneoPuertaTest#celadorRegistraIncidente` | Migrada |
| `test_incidente_sin_detalles_no_se_registra` | `EscaneoPuertaTest#incidenteSinDetallesNoSeRegistra` | Migrada |
| `test_aprendiz_no_registra_incidente` | `EscaneoPuertaTest#aprendizNoRegistraIncidente` | Migrada |
| `test_entidad_id_no_numerico_no_revienta` | `EscaneoPuertaTest#entidadIdNoNumericoNoRevienta` | Migrada |

### tests/modulos/test_fichas.py

Java: `modulos.FichasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_aprendiz_hereda_programa_y_fecha` | `FichasTest.HerenciaDesdeLaFicha#elAprendizHeredaProgramaYFecha` | Migrada |
| `test_cambiar_la_ficha_cambia_el_carnet_de_todos_sus_aprendices` | `FichasTest.HerenciaDesdeLaFicha#cambiarLaFichaCambiaElCarnetDeTodosSusAprendices` | Migrada |
| `test_sin_ficha_enlazada_se_usa_el_texto_historico` | `FichasTest.HerenciaDesdeLaFicha#sinFichaEnlazadaSeUsaElTextoHistorico` | Migrada |
| `test_una_ficha_sin_fecha_no_revienta` | `FichasTest.HerenciaDesdeLaFicha#unaFichaSinFechaNoRevienta` | Migrada |
| `test_al_elegir_ficha_se_copian_numero_y_programa` | `FichasTest.ElAprendizEligeFichaEnSuPerfil#alElegirFichaSeCopianNumeroYPrograma` | Migrada |
| `test_una_ficha_inexistente_se_rechaza` | `FichasTest.ElAprendizEligeFichaEnSuPerfil#unaFichaInexistenteSeRechaza` | Migrada |
| `test_el_aprendiz_no_puede_escribir_su_programa` | `FichasTest.ElAprendizEligeFichaEnSuPerfil#elAprendizNoPuedeEscribirSuPrograma` | Migrada |
| `test_un_admin_entra` | `FichasTest.PermisosDeLaPantallaDeFichas#unAdminEntra` | Migrada |
| `test_quien_no_es_admin_no_entra` (casos: Aprendiz, Instructor, Celador, Administrativo, Administrador) | `FichasTest.PermisosDeLaPantallaDeFichas#quienNoEsAdminNoEntra` | Migrada |
| `test_sin_sesion_redirige_al_login` | `FichasTest.PermisosDeLaPantallaDeFichas#sinSesionRedirigeAlLogin` | Migrada |
| `test_las_acciones_de_escritura_tambien_exigen_admin` (casos: crear, editar, archivar) | `FichasTest.PermisosDeLaPantallaDeFichas#lasAccionesDeEscrituraTambienExigenAdmin` | Migrada |
| `test_crear_editar_y_archivar` | `FichasTest.GestionDeFichas#crearEditarYArchivar` | Migrada |
| `test_no_se_admiten_numeros_de_ficha_invalidos` | `FichasTest.GestionDeFichas#noSeAdmitenNumerosDeFichaInvalidos` | Migrada |
| `test_no_se_repite_el_numero_de_ficha` | `FichasTest.GestionDeFichas#noSeRepiteElNumeroDeFicha` | Migrada |
| `test_una_fecha_invalida_no_crea_la_ficha` | `FichasTest.GestionDeFichas#unaFechaInvalidaNoCreaLaFicha` | Migrada |
| `test_archivar_no_borra_la_ficha_de_sus_aprendices` | `FichasTest.GestionDeFichas#archivarNoBorraLaFichaDeSusAprendices` | Migrada |
| `test_al_crear_usuario_se_enlaza_la_ficha_existente` | `FichasTest.AltaDesdeAdministracion#alCrearUsuarioSeEnlazaLaFichaExistente` | Migrada |
| `test_una_ficha_no_registrada_no_se_inventa` | `FichasTest.AltaDesdeAdministracion#unaFichaNoRegistradaNoSeInventa` | Migrada |

### tests/modulos/test_historial_persona.py

Java: `modulos.HistorialAsistenciaTest`, `modulos.HistorialPersonaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_aprendiz_ve_su_propio_historial` | `HistorialPersonaTest#aprendizVeSuPropioHistorial` | Migrada |
| `test_aprendiz_no_ve_el_historial_de_otro` | `HistorialPersonaTest#aprendizNoVeElHistorialDeOtro` | Migrada |
| `test_api_niega_con_403_el_historial_ajeno` | `HistorialPersonaTest#apiNiegaCon403ElHistorialAjeno` | Migrada |
| `test_aprendiz_no_puede_listar_una_ficha_completa` | `HistorialPersonaTest#aprendizNoPuedeListarUnaFichaCompleta` | Migrada |
| `test_celador_consulta_a_cualquiera` | `HistorialPersonaTest#celadorConsultaACualquiera` | Migrada |
| `test_instructor_consulta_su_ficha` | `HistorialPersonaTest#instructorConsultaSuFicha` | Migrada |
| `test_anonimo_es_redirigido_al_login` | `HistorialPersonaTest#anonimoEsRedirigidoAlLogin` | Migrada |
| `test_entrada_y_salida_se_emparejan_con_permanencia` | `HistorialPersonaTest#entradaYSalidaSeEmparejanConPermanencia` | Migrada |
| `test_entrada_de_hoy_sin_salida_es_alguien_que_sigue_adentro` | `HistorialPersonaTest#entradaDeHoySinSalidaEsAlguienQueSigueAdentro` | Migrada |
| `test_entrada_antigua_sin_salida_si_es_anomalia` | `HistorialPersonaTest#entradaAntiguaSinSalidaSiEsAnomalia` | Migrada |
| `test_dos_entradas_seguidas_dejan_la_primera_sin_salida` | `HistorialPersonaTest#dosEntradasSeguidasDejanLaPrimeraSinSalida` | Migrada |
| `test_salida_sin_entrada_previa_se_conserva` | `HistorialPersonaTest#salidaSinEntradaPreviaSeConserva` | Migrada |
| `test_equipos_se_traducen_a_nombres_y_toleran_borrados` | `HistorialPersonaTest#equiposSeTraducenANombresYToleranBorrados` | Migrada |
| `test_cierre_de_medianoche_sin_operador_se_marca` | `HistorialPersonaTest#cierreDeMedianocheSinOperadorSeMarca` | Migrada |
| `test_salida_real_a_las_2359_no_es_cierre_automatico` | `HistorialPersonaTest#salidaRealALas2359NoEsCierreAutomatico` | Migrada |
| `test_no_se_mezclan_accesos_de_visitantes` | `HistorialPersonaTest#noSeMezclanAccesosDeVisitantes` | Migrada |
| `test_los_accesos_no_se_cruzan_entre_personas` | `HistorialPersonaTest#losAccesosNoSeCruzanEntrePersonas` | Migrada |
| `test_dias_esperados_solo_cuenta_habiles_y_no_el_futuro` | `HistorialAsistenciaTest#diasEsperadosSoloCuentaHabilesYNoElFuturo` | Migrada |
| `test_dias_esperados_puede_incluir_fin_de_semana` | `HistorialAsistenciaTest#diasEsperadosPuedeIncluirFinDeSemana` | Migrada |
| `test_asistencias_y_faltas_del_periodo` | `HistorialAsistenciaTest#asistenciasYFaltasDelPeriodo` | Migrada |
| `test_varias_entradas_el_mismo_dia_cuentan_un_solo_dia` | `HistorialAsistenciaTest#variasEntradasElMismoDiaCuentanUnSoloDia` | Migrada |
| `test_periodo_fuera_de_rango_no_trae_movimientos` | `HistorialAsistenciaTest#periodoFueraDeRangoNoTraeMovimientos` | Migrada |
| `test_aprendiz_recien_vinculado_con_asistencia_perfecta` | `HistorialAsistenciaTest#aprendizRecienVinculadoConAsistenciaPerfecta` | Migrada |
| `test_egresado_no_acumula_faltas_tras_terminar_su_ficha` | `HistorialAsistenciaTest#egresadoNoAcumulaFaltasTrasTerminarSuFicha` | Migrada |
| `test_sin_ningun_ingreso_se_avisa_que_no_hay_referencia` | `HistorialAsistenciaTest#sinNingunIngresoSeAvisaQueNoHayReferencia` | Migrada |
| `test_periodo_anterior_a_la_vinculacion_no_da_porcentaje` | `HistorialAsistenciaTest#periodoAnteriorALaVinculacionNoDaPorcentaje` | Migrada |
| `test_la_salida_de_madrugada_marca_asistido_ese_dia` | `HistorialAsistenciaTest#laSalidaDeMadrugadaMarcaAsistidoEseDia` | Migrada |
| `test_celador_nocturno_no_acumula_una_falta_por_noche` | `HistorialAsistenciaTest#celadorNocturnoNoAcumulaUnaFaltaPorNoche` | Migrada |
| `test_asistir_los_siete_dias_no_pasa_de_cien` | `HistorialAsistenciaTest#asistirLosSieteDiasNoPasaDeCien` | Migrada |
| `test_asistir_solo_en_fin_de_semana_no_inventa_asistencia` | `HistorialAsistenciaTest#asistirSoloEnFinDeSemanaNoInventaAsistencia` | Migrada |
| `test_contando_todos_los_dias_el_fin_de_semana_si_suma` | `HistorialAsistenciaTest#contandoTodosLosDiasElFinDeSemanaSiSuma` | Migrada |
| `test_usa_la_tasa_y_no_el_conteo_bruto` | `HistorialAsistenciaTest#usaLaTasaYNoElConteoBruto` | Migrada |
| `test_quien_nunca_asiste_no_tiene_un_dia_peor` | `HistorialAsistenciaTest#quienNuncaAsisteNoTieneUnDiaPeor` | Migrada |
| `test_empate_entre_dos_dias_no_devuelve_el_primero` | `HistorialAsistenciaTest#empateEntreDosDiasNoDevuelveElPrimero` | Migrada |
| `test_periodo_corto_no_afirma_un_patron` | `HistorialAsistenciaTest#periodoCortoNoAfirmaUnPatron` | Migrada |
| `test_sin_faltas_no_hay_dia_peor` | `HistorialAsistenciaTest#sinFaltasNoHayDiaPeor` | Migrada |
| `test_entrada_el_ultimo_dia_conserva_su_salida` | `HistorialAsistenciaTest#entradaElUltimoDiaConservaSuSalida` | Migrada |
| `test_salida_el_primer_dia_conserva_su_entrada` | `HistorialAsistenciaTest#salidaElPrimerDiaConservaSuEntrada` | Migrada |
| `test_movimiento_totalmente_fuera_del_rango_no_se_cuela` | `HistorialPersonaTest#movimientoTotalmenteFueraDelRangoNoSeCuela` | Migrada |
| `test_rango_demasiado_amplio_se_recorta_y_se_avisa` | `HistorialPersonaTest#rangoDemasiadoAmplioSeRecortaYSeAvisa` | Migrada |
| `test_rango_normal_no_se_recorta` | `HistorialPersonaTest#rangoNormalNoSeRecorta` | Migrada |
| `test_exceso_de_accesos_deja_personas_fuera_en_vez_de_traerlas_a_medias` | `HistorialPersonaTest#excesoDeAccesosDejaPersonasFueraEnVezDeTraerlasAMedias` | Migrada |
| `test_sin_filtros_muestra_el_formulario_vacio` | `HistorialPersonaTest#sinFiltrosMuestraElFormularioVacio` | Migrada |
| `test_la_pagina_muestra_el_nombre_y_los_equipos` | `HistorialPersonaTest#laPaginaMuestraElNombreYLosEquipos` | Migrada |
| `test_la_pagina_no_pinta_un_porcentaje_sin_dias_exigibles` | `HistorialPersonaTest#laPaginaNoPintaUnPorcentajeSinDiasExigibles` | Migrada |
| `test_la_pagina_explica_el_empate_en_vez_de_afirmar_un_dia` | `HistorialPersonaTest#laPaginaExplicaElEmpateEnVezDeAfirmarUnDia` | Migrada |
| `test_la_pagina_avisa_cuando_recorta_el_rango` | `HistorialPersonaTest#laPaginaAvisaCuandoRecortaElRango` | Migrada |
| `test_la_pagina_avisa_de_las_personas_no_analizadas` | `HistorialPersonaTest#laPaginaAvisaDeLasPersonasNoAnalizadas` | Migrada |
| `test_rango_de_fechas_invertido_no_rompe_la_vista` | `HistorialPersonaTest#rangoDeFechasInvertidoNoRompeLaVista` | Migrada |

### tests/modulos/test_importacion_excel.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_una_celda_vacia_no_convierte_los_documentos_en_decimales` | — | Programada fase 5 |
| `test_la_ficha_tampoco_se_corrompe` | — | Programada fase 5 |
| `test_un_documento_invalido_no_se_guarda` | — | Programada fase 5 |
| `test_el_documento_se_normaliza_como_en_porteria` | — | Programada fase 5 |
| `test_una_fila_que_pide_admin_se_degrada_a_usuario` | — | Programada fase 5 |
| `test_un_cargo_inventado_se_degrada_a_aprendiz` | — | Programada fase 5 |
| `test_la_importacion_no_la_puede_lanzar_cualquiera` | — | Programada fase 5 |
| `test_un_correo_repetido_se_omite_y_el_resto_entra` | — | Programada fase 5 |
| `test_un_documento_repetido_no_tumba_el_lote` | — | Programada fase 5 |
| `test_una_fila_corrupta_no_impide_que_se_importen_las_demas` | — | Programada fase 5 |
| `test_las_filas_vacias_del_final_se_omiten_sin_avisos` | — | Programada fase 5 |
| `test_cada_fila_recibe_una_contrasena_distinta` | — | Programada fase 5 |
| `test_los_hashes_no_se_repiten` | — | Programada fase 5 |
| `test_el_lote_queda_registrado` | — | Programada fase 5 |

### tests/modulos/test_integridad.py

Java: `modulos.IntegridadTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_con_asistencias` | `IntegridadTest#conAsistencias` | Migrada |
| `test_instructor_con_clases_dictadas` | `IntegridadTest#instructorConClasesDictadas` | Migrada |
| `test_con_mensajes` | `IntegridadTest#conMensajes` | Migrada |
| `test_operador_de_porteria` | `IntegridadTest#operadorDePorteria` | Migrada |
| `test_celador_con_turnos_de_la_tabla_retirada` | — | Descartada |
| `test_equipo_que_cruzo_la_porteria` | `IntegridadTest#equipoQueCruzoLaPorteria` | Migrada |
| `test_indice_compuesto_del_escaner` | `IntegridadTest#indiceCompuestoDelEscaner` | Migrada |
| `test_indice_por_fecha_de_acceso` | `IntegridadTest#indicePorFechaDeAcceso` | Migrada |
| `test_indices_de_claves_ajenas` | `IntegridadTest#indicesDeClavesAjenas` | Migrada |
| `test_serial_de_objeto_externo_es_unico` | `IntegridadTest#serialDeObjetoExternoEsUnico` | Migrada |

Descartada: la base nueva no hereda la tabla turnos_celador de Portería 2 (decisión del 2026-09-23 de arrancar sin sus datos).

### tests/modulos/test_limites.py

Java: `modulos.LimitesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_todos_los_endpoints_limitados_existen` | `LimitesTest#todosLosEndpointsLimitadosExisten` | Migrada |
| `test_todos_los_endpoints_exentos_existen` | `LimitesTest#todosLosEndpointsExentosExisten` | Migrada |
| `test_el_limite_salta_por_ip` | `LimitesTest#elLimiteSaltaPorIp` | Migrada |
| `test_el_mensaje_json_es_entendible` | `LimitesTest#elMensajeJsonEsEntendible` | Migrada |
| `test_el_mensaje_html_es_una_pagina` | `LimitesTest#elMensajeHtmlEsUnaPagina` | Migrada |
| `test_cada_ip_tiene_su_propio_contador` | `LimitesTest#cadaIpTieneSuPropioContador` | Migrada |
| `test_muchas_verificaciones_seguidas` | `LimitesTest#muchasVerificacionesSeguidas` | Migrada |
| `test_muchos_movimientos_seguidos` | `LimitesTest#muchosMovimientosSeguidos` | Migrada |
| `test_el_centro_de_ayuda_se_limita` | `LimitesTest#elCentroDeAyudaSeLimita` | Migrada |
| `test_apagado_el_registro_sigue_funcionando` | — | Programada fase 5 |
| `test_apagado_la_recuperacion_sigue_funcionando` | — | Programada fase 5 |
| `test_apagado_el_widget_no_se_pinta` | — | Programada fase 5 |
| `test_encendido_sin_solucion_no_registra` | — | Programada fase 5 |
| `test_encendido_con_solucion_si_registra` | — | Programada fase 5 |
| `test_una_solucion_no_sirve_dos_veces` | — | Programada fase 5 |
| `test_una_solucion_inventada_no_pasa` | — | Programada fase 5 |
| `test_encendido_el_widget_se_pinta` | — | Programada fase 5 |

### tests/modulos/test_mensajes.py

Java: `modulos.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_puede_enviar_un_mensaje` | `MensajesTest.UsuarioEscribe#puedeEnviarUnMensaje` | Migrada |
| `test_un_mensaje_vacio_no_se_guarda` | `MensajesTest.UsuarioEscribe#unMensajeVacioNoSeGuarda` | Migrada |
| `test_solo_ve_su_propio_hilo` | `MensajesTest.UsuarioEscribe#soloVeSuPropioHilo` | Migrada |
| `test_no_puede_entrar_a_la_bandeja_del_admin` | `MensajesTest.UsuarioEscribe#noPuedeEntrarALaBandejaDelAdmin` | Migrada |
| `test_no_puede_escribirle_a_otro_como_si_fuera_admin` | `MensajesTest.UsuarioEscribe#noPuedeEscribirleAOtroComoSiFueraAdmin` | Migrada |
| `test_el_admin_escribe_a_una_persona` | `MensajesTest.AdminResponde#elAdminEscribeAUnaPersona` | Migrada |
| `test_la_persona_ve_lo_que_le_escribio_el_admin` | `MensajesTest.AdminResponde#laPersonaVeLoQueLeEscribioElAdmin` | Migrada |
| `test_abrir_el_hilo_marca_los_mensajes_como_leidos` | `MensajesTest.AdminResponde#abrirElHiloMarcaLosMensajesComoLeidos` | Migrada |
| `test_escribir_a_alguien_que_no_existe` | `MensajesTest.AdminResponde#escribirAAlguienQueNoExiste` | Migrada |
| `test_al_rechazar_la_foto_queda_el_motivo_en_el_hilo` | `MensajesTest.RechazoDejaMensaje#alRechazarLaFotoQuedaElMotivoEnElHilo` | Migrada |
| `test_al_aprobar_tambien_queda_constancia` | `MensajesTest.RechazoDejaMensaje#alAprobarTambienQuedaConstancia` | Migrada |
| `test_cualquiera_puede_ver_las_preguntas_frecuentes` | `MensajesTest.CentroDeAyuda#cualquieraPuedeVerLasPreguntasFrecuentes` | Migrada |
| `test_contactar_abre_la_conversacion` | `MensajesTest.CentroDeAyuda#contactarAbreLaConversacion` | Migrada |
| `test_sin_detalle_no_se_abre_nada` | `MensajesTest.CentroDeAyuda#sinDetalleNoSeAbreNada` | Migrada |
| `test_el_administrativo_puede_asesorar` | `MensajesTest.QuienPuedeAsesorar#elAdministrativoPuedeAsesorar` | Migrada |
| `test_el_aprendiz_no_puede_asesorar` | `MensajesTest.QuienPuedeAsesorar#elAprendizNoPuedeAsesorar` | Migrada |
| `test_el_celador_no_puede_asesorar` | `MensajesTest.QuienPuedeAsesorar#elCeladorNoPuedeAsesorar` | Migrada |
| `test_el_instructor_no_puede_asesorar` | `MensajesTest.QuienPuedeAsesorar#elInstructorNoPuedeAsesorar` | Migrada |

### tests/modulos/test_minimizacion.py

Java: `modulos.MinimizacionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_la_bandeja_del_asesor_no_muestra_la_cedula_completa` | `MinimizacionTest#laBandejaDelAsesorNoMuestraLaCedulaCompleta` | Migrada |
| `test_el_hilo_abierto_no_muestra_cedula_completa_ni_correo` | `MinimizacionTest#elHiloAbiertoNoMuestraCedulaCompletaNiCorreo` | Migrada |
| `test_porteria_si_ve_el_documento_completo` | `MinimizacionTest#porteriaSiVeElDocumentoCompleto` | Migrada |
| `test_no_muestra_el_correo_en_las_tarjetas` | `MinimizacionTest#noMuestraElCorreoEnLasTarjetas` | Migrada |
| `test_la_lista_esta_paginada` | `MinimizacionTest#laListaEstaPaginada` | Migrada |
| `test_sin_fechas_no_exporta_y_avisa` | `MinimizacionTest#sinFechasNoExportaYAvisa` | Migrada |
| `test_con_fechas_exporta_y_deja_constancia_en_auditoria` | `MinimizacionTest#conFechasExportaYDejaConstanciaEnAuditoria` | Migrada |
| `test_rango_invertido_se_rechaza` | `MinimizacionTest#rangoInvertidoSeRechaza` | Migrada |
| `test_el_historial_html_esta_en_el_catalogo_de_limites` | `MinimizacionTest#elHistorialHtmlEstaEnElCatalogoDeLimites` | Migrada |
| `test_el_escaner_de_porteria_sigue_libre` | `MinimizacionTest#elEscanerDePorteriaSigueLibre` | Migrada |
| `test_aprobar_dos_veces_no_duplica_mensajes` | `MinimizacionTest#aprobarDosVecesNoDuplicaMensajes` | Migrada |
| `test_la_foto_sigue_aprobada_tras_el_segundo_intento` | `MinimizacionTest#laFotoSigueAprobadaTrasElSegundoIntento` | Migrada |

### tests/modulos/test_pases.py

Java: `modulos.PasesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_celador_crea_visitante` | `PasesTest#celadorCreaVisitante` | Migrada |
| `test_aprendiz_no_crea_visitante` | `PasesTest#aprendizNoCreaVisitante` | Migrada |
| `test_sin_documento_no_crea_visitante` | `PasesTest#sinDocumentoNoCreaVisitante` | Migrada |
| `test_visitante_existente_se_reactiva_en_vez_de_duplicar` | `PasesTest#visitanteExistenteSeReactivaEnVezDeDuplicar` | Migrada |
| `test_celador_crea_vehiculo` | `PasesTest#celadorCreaVehiculo` | Migrada |
| `test_vehiculo_sena_usa_prefijo_distinto` | `PasesTest#vehiculoSenaUsaPrefijoDistinto` | Migrada |
| `test_placa_vacia_no_crea_vehiculo` | `PasesTest#placaVaciaNoCreaVehiculo` | Migrada |
| `test_aprendiz_no_crea_vehiculo` | `PasesTest#aprendizNoCreaVehiculo` | Migrada |
| `test_celador_crea_objeto_con_serial` | `PasesTest#celadorCreaObjetoConSerial` | Migrada |
| `test_objeto_sin_serial_genera_uno` | `PasesTest#objetoSinSerialGeneraUno` | Migrada |
| `test_descripcion_vacia_no_crea_objeto` | `PasesTest#descripcionVaciaNoCreaObjeto` | Migrada |
| `test_aprendiz_no_crea_objeto` | `PasesTest#aprendizNoCreaObjeto` | Migrada |
| `test_celador_ve_formulario_de_edicion` | `PasesTest#celadorVeFormularioDeEdicion` | Migrada |
| `test_editar_objeto_inexistente_da_404` | `PasesTest#editarObjetoInexistenteDa404` | Migrada |
| `test_aprendiz_no_ve_formulario_de_edicion` | `PasesTest#aprendizNoVeFormularioDeEdicion` | Migrada |
| `test_celador_actualiza_objeto` | `PasesTest#celadorActualizaObjeto` | Migrada |
| `test_aprendiz_no_actualiza_objeto` | `PasesTest#aprendizNoActualizaObjeto` | Migrada |
| `test_celador_elimina_objeto` | `PasesTest#celadorEliminaObjeto` | Migrada |
| `test_aprendiz_no_elimina_objeto` | `PasesTest#aprendizNoEliminaObjeto` | Migrada |
| `test_un_celador_no_deberia_poder_editar_el_pase_creado_por_otro` | `PasesTest#unCeladorNoDeberiaPoderEditarElPaseCreadoPorOtro` | Migrada |
| `test_entrada_de_visitante_se_registra` | `PasesTest#entradaDeVisitanteSeRegistra` | Migrada |
| `test_doble_entrada_de_visitante_se_rechaza_y_audita` | `PasesTest#dobleEntradaDeVisitanteSeRechazaYAudita` | Migrada |
| `test_salida_de_visitante_sin_entrada_se_rechaza` | `PasesTest#salidaDeVisitanteSinEntradaSeRechaza` | Migrada |
| `test_entrada_de_vehiculo_se_registra` | `PasesTest#entradaDeVehiculoSeRegistra` | Migrada |
| `test_doble_entrada_de_vehiculo_se_rechaza_y_audita` | `PasesTest#dobleEntradaDeVehiculoSeRechazaYAudita` | Migrada |
| `test_salida_de_vehiculo_sin_entrada_se_rechaza` | `PasesTest#salidaDeVehiculoSinEntradaSeRechaza` | Migrada |
| `test_entrada_y_salida_completa_de_visitante` | `PasesTest#entradaYSalidaCompletaDeVisitante` | Migrada |

### tests/modulos/test_porteria.py

Java: `modulos.PorteriaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_aprendiz_no_entra_al_escaner` | `PorteriaTest#aprendizNoEntraAlEscaner` | Migrada |
| `test_celador_entra_al_escaner` | `PorteriaTest#celadorEntraAlEscaner` | Migrada |
| `test_aprendiz_no_registra_movimientos` | `PorteriaTest#aprendizNoRegistraMovimientos` | Migrada |
| `test_aprendiz_no_gestiona_asistencia` | `PorteriaTest#aprendizNoGestionaAsistencia` | Migrada |
| `test_celador_no_entra_a_gestion_de_usuarios` | `PorteriaTest#celadorNoEntraAGestionDeUsuarios` | Migrada |
| `test_entrada_valida_se_registra` | `PorteriaTest#entradaValidaSeRegistra` | Migrada |
| `test_doble_entrada_se_rechaza_y_se_audita` | `PorteriaTest#dobleEntradaSeRechazaYSeAudita` | Migrada |
| `test_salida_sin_entrada_se_rechaza` | `PorteriaTest#salidaSinEntradaSeRechaza` | Migrada |
| `test_movimiento_invalido_se_rechaza` | `PorteriaTest#movimientoInvalidoSeRechaza` | Migrada |
| `test_entidad_inventada_no_crea_registro` | `PorteriaTest#entidadInventadaNoCreaRegistro` | Migrada |
| `test_el_acceso_registra_quien_lo_hizo` | `PorteriaTest#elAccesoRegistraQuienLoHizo` | Migrada |
| `test_no_se_pueden_marcar_equipos_de_otro` | `PorteriaTest#noSePuedenMarcarEquiposDeOtro` | Migrada |
| `test_equipo_propio_cambia_de_estado` | `PorteriaTest#equipoPropioCambiaDeEstado` | Migrada |
| `test_no_se_puede_borrar_el_equipo_de_otro` | `PorteriaTest#noSePuedeBorrarElEquipoDeOtro` | Migrada |
| `test_borrar_por_get_ya_no_funciona` | `PorteriaTest#borrarPorGetYaNoFunciona` | Migrada |

### tests/modulos/test_reportes_permisos.py

Java: `modulos.ReportesPermisosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_aprendiz_no_entra_a_analytics` | `ReportesPermisosTest#aprendizNoEntraAAnalytics` | Migrada |
| `test_celador_entra_a_analytics` | `ReportesPermisosTest#celadorEntraAAnalytics` | Migrada |
| `test_admin_entra_a_analytics` | `ReportesPermisosTest#adminEntraAAnalytics` | Migrada |
| `test_no_autenticado_no_entra_a_analytics` | `ReportesPermisosTest#noAutenticadoNoEntraAAnalytics` | Migrada |
| `test_aprendiz_no_entra` | `ReportesPermisosTest#aprendizNoEntra` | Migrada |
| `test_celador_no_entra` | `ReportesPermisosTest#celadorNoEntra` | Migrada |
| `test_admin_entra` | `ReportesPermisosTest#adminEntra` | Migrada |
| `test_admin_busca_por_ficha` | `ReportesPermisosTest#adminBuscaPorFicha` | Migrada |
| `test_aprendiz_no_exporta` | `ReportesPermisosTest#aprendizNoExporta` | Migrada |
| `test_celador_exporta` | `ReportesPermisosTest#celadorExporta` | Migrada |
| `test_sin_rango_de_fechas_no_exporta` | `ReportesPermisosTest#sinRangoDeFechasNoExporta` | Migrada |
| `test_rango_invertido_no_exporta` | `ReportesPermisosTest#rangoInvertidoNoExporta` | Migrada |
| `test_exportar_deja_constancia_en_auditoria` | `ReportesPermisosTest#exportarDejaConstanciaEnAuditoria` | Migrada |
| `test_nombre_con_formula_se_neutraliza` | `ReportesPermisosTest#nombreConFormulaSeNeutraliza` | Migrada |
| `test_documento_con_formula_se_neutraliza` | `ReportesPermisosTest#documentoConFormulaSeNeutraliza` | Migrada |
| `test_nombre_normal_no_se_toca` | `ReportesPermisosTest#nombreNormalNoSeToca` | Migrada |

### tests/modulos/test_revision_fotos.py

Java: `modulos.RevisionFotosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_un_aprendiz_no_entra_a_la_cola` | `RevisionFotosTest#unAprendizNoEntraALaCola` | Migrada |
| `test_un_celador_tampoco` | `RevisionFotosTest#unCeladorTampoco` | Migrada |
| `test_un_aprendiz_no_puede_aprobar_su_propia_foto` | `RevisionFotosTest#unAprendizNoPuedeAprobarSuPropiaFoto` | Migrada |
| `test_el_admin_ve_la_cola` | `RevisionFotosTest#elAdminVeLaCola` | Migrada |
| `test_aprobar_deja_la_foto_aprobada_y_audita` | `RevisionFotosTest#aprobarDejaLaFotoAprobadaYAudita` | Migrada |
| `test_rechazar_exige_motivo` | `RevisionFotosTest#rechazarExigeMotivo` | Migrada |
| `test_rechazar_borra_la_foto_y_guarda_el_motivo` | `RevisionFotosTest#rechazarBorraLaFotoYGuardaElMotivo` | Migrada |
| `test_decision_invalida_se_rechaza` | `RevisionFotosTest#decisionInvalidaSeRechaza` | Migrada |
| `test_usuario_inexistente` | `RevisionFotosTest#usuarioInexistente` | Migrada |
| `test_sin_aprobar_no_hay_carnet` | `RevisionFotosTest#sinAprobarNoHayCarnet` | Migrada |
| `test_con_la_foto_aprobada_si_hay_carnet` | `RevisionFotosTest#conLaFotoAprobadaSiHayCarnet` | Migrada |
| `test_un_usuario_nuevo_empieza_sin_foto` | `RevisionFotosTest#unUsuarioNuevoEmpiezaSinFoto` | Migrada |
| `test_la_api_del_escaner_dice_si_la_foto_esta_aprobada` | `RevisionFotosTest#laApiDelEscanerDiceSiLaFotoEstaAprobada` | Migrada |
| `test_un_404_no_revienta_para_un_usuario_autenticado` | `RevisionFotosTest#un404NoRevientaParaUnUsuarioAutenticado` | Migrada |

### tests/modulos/test_seguridad.py

Java: `modulos.SeguridadTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_rechaza_vacia` | `SeguridadTest#rechazaVacia` | Migrada |
| `test_rechaza_menos_de_ocho` (casos: 1234, abc, Abc123, 1234567) | `SeguridadTest#rechazaMenosDeOcho` | Migrada |
| `test_rechaza_solo_numeros_o_solo_letras` | `SeguridadTest#rechazaSoloNumerosOSoloLetras` | Migrada |
| `test_rechaza_confirmacion_distinta` | `SeguridadTest#rechazaConfirmacionDistinta` | Migrada |
| `test_acepta_valida` | `SeguridadTest#aceptaValida` | Migrada |
| `test_iguales` | `SeguridadTest#iguales` | Migrada |
| `test_distintos` | `SeguridadTest#distintos` | Migrada |
| `test_vacios_no_coinciden` | `SeguridadTest#vaciosNoCoinciden` | Migrada |
| `test_lista_vacia_permite_todo` | — | Programada fase 5 |
| `test_filtra_por_dominio` | — | Programada fase 5 |
| `test_no_se_enganya_con_subcadenas` | — | Programada fase 5 |
| `test_quita_etiquetas` | `SeguridadTest#quitaEtiquetas` | Migrada |
| `test_conserva_texto_normal` | `SeguridadTest#conservaTextoNormal` | Migrada |
| `test_parsea_datetime_sin_desplazar` | `SeguridadTest#parseaDatetimeSinDesplazar` | Migrada |
| `test_parsea_cadena_de_sqlite` | `SeguridadTest#parseaCadenaDeSqlite` | Migrada |
| `test_devuelve_none_si_no_puede` | `SeguridadTest#devuelveNoneSiNoPuede` | Migrada |
| `test_una_entrada_de_la_manana_no_cae_al_dia_anterior` | `SeguridadTest#unaEntradaDeLaMananaNoCaeAlDiaAnterior` | Migrada |
| `test_hora_colombia_es_naive` | `SeguridadTest#horaColombiaEsNaive` | Migrada |

### tests/modulos/test_tutorial.py

Java: `modulos.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_se_marca_el_tutorial_como_visto` | `TutorialTest.MarcarComoVisto#seMarcaElTutorialComoVisto` | Migrada |
| `test_marcarlo_dos_veces_no_falla` | `TutorialTest.MarcarComoVisto#marcarloDosVecesNoFalla` | Migrada |
| `test_sin_sesion_no_se_puede_marcar` | `TutorialTest.MarcarComoVisto#sinSesionNoSePuedeMarcar` | Migrada |
| `test_no_se_puede_marcar_el_de_otra_persona` | `TutorialTest.MarcarComoVisto#noSePuedeMarcarElDeOtraPersona` | Migrada |
| `test_la_pagina_del_tutorial_carga` | `TutorialTest.VersionEnTexto#laPaginaDelTutorialCarga` | Migrada |
| `test_sin_sesion_redirige_al_login` | `TutorialTest.VersionEnTexto#sinSesionRedirigeAlLogin` | Migrada |
| `test_ofrece_relanzar_el_recorrido_guiado` | `TutorialTest.VersionEnTexto#ofreceRelanzarElRecorridoGuiado` | Migrada |
| `test_aparece_con_el_perfil_incompleto` | `TutorialTest.BotonDeAcceso#apareceConElPerfilIncompleto` | Migrada |
| `test_desaparece_con_el_perfil_completo` | `TutorialTest.BotonDeAcceso#desapareceConElPerfilCompleto` | Migrada |
| `test_el_script_indica_que_no_se_ha_visto` | `TutorialTest.RecorridoGuiado#elScriptIndicaQueNoSeHaVisto` | Migrada |
| `test_el_script_indica_que_ya_se_vio` | `TutorialTest.RecorridoGuiado#elScriptIndicaQueYaSeVio` | Migrada |

### tests/roles/admin/test_ambientes.py

Java: `roles.admin.AmbientesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_ambientes` | `AmbientesTest#entraAAmbientes` | Migrada |
| `test_ve_el_detalle_de_un_ambiente` | `AmbientesTest#veElDetalleDeUnAmbiente` | Migrada |

### tests/roles/admin/test_asistencia.py

Java: `roles.admin.AsistenciaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_asistencia` | `AsistenciaTest#entraAAsistencia` | Migrada |
| `test_busca_una_ficha` | `AsistenciaTest#buscaUnaFicha` | Migrada |

### tests/roles/admin/test_ayuda.py

Java: `roles.admin.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/admin/test_bandeja_mensajes.py

Java: `roles.admin.BandejaMensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_la_bandeja` | `BandejaMensajesTest#entraALaBandeja` | Migrada |
| `test_abre_la_conversacion_de_otra_persona` | `BandejaMensajesTest#abreLaConversacionDeOtraPersona` | Migrada |
| `test_responde_a_otra_persona` | `BandejaMensajesTest#respondeAOtraPersona` | Migrada |

### tests/roles/admin/test_carnet.py

Java: `roles.admin.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/admin/test_cerrar_sesion.py

Java: `roles.admin.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/admin/test_comunicados.py

Java: `roles.admin.ComunicadosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_comunicados` | `ComunicadosTest#entraAComunicados` | Migrada |
| `test_enviar_sin_destinatarios_se_rechaza` | `ComunicadosTest#enviarSinDestinatariosSeRechaza` | Migrada |

### tests/roles/admin/test_equipos.py

Java: `roles.admin.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/admin/test_escaner.py

Java: `roles.admin.EscanerTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_escaner` | `EscanerTest#entraAlEscaner` | Migrada |
| `test_verifica_un_documento` | `EscanerTest#verificaUnDocumento` | Migrada |
| `test_registra_una_entrada` | `EscanerTest#registraUnaEntrada` | Migrada |

### tests/roles/admin/test_fichas.py

Java: `roles.admin.FichasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_fichas` | `FichasTest#entraAFichas` | Migrada |
| `test_crea_una_ficha` | `FichasTest#creaUnaFicha` | Migrada |

### tests/roles/admin/test_gestion_usuarios.py

Java: `roles.admin.GestionUsuariosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_gestion_de_usuarios` | `GestionUsuariosTest#entraAGestionDeUsuarios` | Migrada |
| `test_crea_un_usuario` | `GestionUsuariosTest#creaUnUsuario` | Migrada |
| `test_no_puede_eliminarse_a_si_mismo` | `GestionUsuariosTest#noPuedeEliminarseASiMismo` | Migrada |

### tests/roles/admin/test_historial_cambios.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_historial_de_cambios` | — | Programada fase 5 |

### tests/roles/admin/test_historial_clases.py

Java: `roles.admin.HistorialClasesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_historial_de_clases` | `HistorialClasesTest#entraAlHistorialDeClases` | Migrada |
| `test_busca_una_ficha` | `HistorialClasesTest#buscaUnaFicha` | Migrada |

### tests/roles/admin/test_historial_ingresos.py

Java: `roles.admin.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#consultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/admin/test_mensajes.py

Java: `roles.admin.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/admin/test_panel.py

Java: `roles.admin.PanelTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_panel` | `PanelTest#entraAlPanel` | Migrada |
| `test_exporta_el_historial` | `PanelTest#exportaElHistorial` | Migrada |

### tests/roles/admin/test_pases.py

Java: `roles.admin.PasesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_pases` | `PasesTest#entraAPases` | Migrada |
| `test_registra_un_visitante` | `PasesTest#registraUnVisitante` | Migrada |
| `test_registra_un_vehiculo` | `PasesTest#registraUnVehiculo` | Migrada |

### tests/roles/admin/test_perfil.py

Java: `roles.admin.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/admin/test_reportes.py

Java: `roles.admin.ReportesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_reportes` (casos: Aprendiz, Instructor, Personal) | `ReportesTest#entraAReportes` | Migrada |

### tests/roles/admin/test_respaldos.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_respaldos` | — | Programada fase 5 |

### tests/roles/admin/test_revision_fotos.py

Java: `roles.admin.RevisionFotosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_revision_de_fotos` (casos: pendiente, aprobada, rechazada, todos) | `RevisionFotosTest#entraARevisionDeFotos` | Migrada |
| `test_decision_invalida_se_rechaza` | `RevisionFotosTest#decisionInvalidaSeRechaza` | Migrada |

### tests/roles/admin/test_tutorial.py

Java: `roles.admin.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/administrador/test_ayuda.py

Java: `roles.administrador.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/administrador/test_bandeja_mensajes.py

Java: `roles.administrador.BandejaMensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_la_bandeja` | `BandejaMensajesTest#entraALaBandeja` | Migrada |
| `test_abre_la_conversacion_de_otra_persona` | `BandejaMensajesTest#abreLaConversacionDeOtraPersona` | Migrada |
| `test_responde_a_otra_persona` | `BandejaMensajesTest#respondeAOtraPersona` | Migrada |

### tests/roles/administrador/test_carnet.py

Java: `roles.administrador.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/administrador/test_cerrar_sesion.py

Java: `roles.administrador.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/administrador/test_equipos.py

Java: `roles.administrador.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/administrador/test_escaner.py

Java: `roles.administrador.EscanerTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_escaner` | `EscanerTest#entraAlEscaner` | Migrada |
| `test_verifica_un_documento` | `EscanerTest#verificaUnDocumento` | Migrada |
| `test_registra_una_entrada` | `EscanerTest#registraUnaEntrada` | Migrada |

### tests/roles/administrador/test_historial_ingresos.py

Java: `roles.administrador.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#consultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/administrador/test_mensajes.py

Java: `roles.administrador.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/administrador/test_panel.py

Java: `roles.administrador.PanelTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_panel` | `PanelTest#entraAlPanel` | Migrada |
| `test_exporta_el_historial` | `PanelTest#exportaElHistorial` | Migrada |

### tests/roles/administrador/test_pases.py

Java: `roles.administrador.PasesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_pases` | `PasesTest#entraAPases` | Migrada |
| `test_registra_un_visitante` | `PasesTest#registraUnVisitante` | Migrada |
| `test_registra_un_vehiculo` | `PasesTest#registraUnVehiculo` | Migrada |

### tests/roles/administrador/test_perfil.py

Java: `roles.administrador.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/administrador/test_reportes.py

Java: `roles.administrador.ReportesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_reportes` (casos: Aprendiz, Instructor, Personal) | `ReportesTest#entraAReportes` | Migrada |

### tests/roles/administrador/test_restringidas.py

Java: `roles.administrador.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: gestion_usuarios, revision_fotos, ambientes, asistencia, comunicados, fichas, historial_clases; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/administrador/test_tutorial.py

Java: `roles.administrador.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/administrativo/test_ayuda.py

Java: `roles.administrativo.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/administrativo/test_bandeja_mensajes.py

Java: `roles.administrativo.BandejaMensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_la_bandeja` | `BandejaMensajesTest#entraALaBandeja` | Migrada |
| `test_abre_la_conversacion_de_otra_persona` | `BandejaMensajesTest#abreLaConversacionDeOtraPersona` | Migrada |
| `test_responde_a_otra_persona` | `BandejaMensajesTest#respondeAOtraPersona` | Migrada |

### tests/roles/administrativo/test_carnet.py

Java: `roles.administrativo.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/administrativo/test_cerrar_sesion.py

Java: `roles.administrativo.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/administrativo/test_equipos.py

Java: `roles.administrativo.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/administrativo/test_historial_ingresos.py

Java: `roles.administrativo.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_no_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#noConsultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/administrativo/test_mensajes.py

Java: `roles.administrativo.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/administrativo/test_perfil.py

Java: `roles.administrativo.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/administrativo/test_restringidas.py

Java: `roles.administrativo.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, ambientes, asistencia, comunicados, fichas, historial_clases; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/administrativo/test_tutorial.py

Java: `roles.administrativo.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/aprendiz/test_ayuda.py

Java: `roles.aprendiz.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/aprendiz/test_carnet.py

Java: `roles.aprendiz.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/aprendiz/test_cerrar_sesion.py

Java: `roles.aprendiz.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/aprendiz/test_equipos.py

Java: `roles.aprendiz.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/aprendiz/test_historial_ingresos.py

Java: `roles.aprendiz.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_no_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#noConsultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/aprendiz/test_mensajes.py

Java: `roles.aprendiz.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/aprendiz/test_perfil.py

Java: `roles.aprendiz.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/aprendiz/test_restringidas.py

Java: `roles.aprendiz.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, ambientes, asistencia, comunicados, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/aprendiz/test_tutorial.py

Java: `roles.aprendiz.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/celador/test_ayuda.py

Java: `roles.celador.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/celador/test_carnet.py

Java: `roles.celador.CarnetTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/celador/test_cerrar_sesion.py

Java: `roles.celador.CerrarSesionTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/celador/test_escaner.py

Java: `roles.celador.EscanerTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_escaner` | `EscanerTest#entraAlEscaner` | Migrada |
| `test_verifica_un_documento` | `EscanerTest#verificaUnDocumento` | Migrada |
| `test_registra_una_entrada` | `EscanerTest#registraUnaEntrada` | Migrada |

### tests/roles/celador/test_historial_ingresos.py

Java: `roles.celador.HistorialIngresosTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#consultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/celador/test_mensajes.py

Java: `roles.celador.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/celador/test_panel.py

Java: `roles.celador.PanelTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_panel` | `PanelTest#entraAlPanel` | Migrada |
| `test_exporta_el_historial` | `PanelTest#exportaElHistorial` | Migrada |

### tests/roles/celador/test_pases.py

Java: `roles.celador.PasesTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_pases` | `PasesTest#entraAPases` | Migrada |
| `test_registra_un_visitante` | `PasesTest#registraUnVisitante` | Migrada |
| `test_registra_un_vehiculo` | `PasesTest#registraUnVehiculo` | Migrada |

### tests/roles/celador/test_perfil.py

Java: `roles.celador.PerfilTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/celador/test_reportes.py

Java: `roles.celador.ReportesTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_reportes` (casos: Aprendiz, Instructor, Personal) | `ReportesTest#entraAReportes` | Migrada |

### tests/roles/celador/test_restringidas.py

Java: `roles.celador.RestringidasTest` (cada prueba corre para celador y porteria)

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: gestion_usuarios, revision_fotos, ambientes, asistencia, comunicados, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |
| `test_no_registra_equipos` | `RestringidasTest#noRegistraEquipos` | Migrada |

### tests/roles/celador/test_tutorial.py

Java: `roles.celador.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/contratista/test_ayuda.py

Java: `roles.contratista.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/contratista/test_carnet.py

Java: `roles.contratista.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/contratista/test_cerrar_sesion.py

Java: `roles.contratista.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/contratista/test_equipos.py

Java: `roles.contratista.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/contratista/test_historial_ingresos.py

Java: `roles.contratista.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_no_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#noConsultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/contratista/test_mensajes.py

Java: `roles.contratista.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/contratista/test_perfil.py

Java: `roles.contratista.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/contratista/test_restringidas.py

Java: `roles.contratista.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, ambientes, asistencia, comunicados, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/contratista/test_tutorial.py

Java: `roles.contratista.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/coordinacion/test_ambientes.py

Java: `roles.coordinacion.AmbientesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_ambientes` | `AmbientesTest#entraAAmbientes` | Migrada |
| `test_ve_el_detalle_de_un_ambiente` | `AmbientesTest#veElDetalleDeUnAmbiente` | Migrada |

### tests/roles/coordinacion/test_ayuda.py

Java: `roles.coordinacion.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/coordinacion/test_carnet.py

Java: `roles.coordinacion.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/coordinacion/test_cerrar_sesion.py

Java: `roles.coordinacion.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/coordinacion/test_equipos.py

Java: `roles.coordinacion.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/coordinacion/test_historial_ingresos.py

Java: `roles.coordinacion.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_no_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#noConsultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/coordinacion/test_mensajes.py

Java: `roles.coordinacion.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/coordinacion/test_perfil.py

Java: `roles.coordinacion.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/coordinacion/test_restringidas.py

Java: `roles.coordinacion.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, asistencia, comunicados, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/coordinacion/test_tutorial.py

Java: `roles.coordinacion.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/funcionario/test_ayuda.py

Java: `roles.funcionario.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/funcionario/test_carnet.py

Java: `roles.funcionario.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/funcionario/test_cerrar_sesion.py

Java: `roles.funcionario.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/funcionario/test_equipos.py

Java: `roles.funcionario.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/funcionario/test_historial_ingresos.py

Java: `roles.funcionario.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_no_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#noConsultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/funcionario/test_mensajes.py

Java: `roles.funcionario.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/funcionario/test_perfil.py

Java: `roles.funcionario.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/funcionario/test_restringidas.py

Java: `roles.funcionario.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, ambientes, asistencia, comunicados, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/funcionario/test_tutorial.py

Java: `roles.funcionario.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/instructor/test_asistencia.py

Java: `roles.instructor.AsistenciaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_asistencia` | `AsistenciaTest#entraAAsistencia` | Migrada |
| `test_busca_una_ficha` | `AsistenciaTest#buscaUnaFicha` | Migrada |

### tests/roles/instructor/test_ayuda.py

Java: `roles.instructor.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/instructor/test_carnet.py

Java: `roles.instructor.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/instructor/test_cerrar_sesion.py

Java: `roles.instructor.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/instructor/test_comunicados.py

Java: `roles.instructor.ComunicadosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_comunicados` | `ComunicadosTest#entraAComunicados` | Migrada |
| `test_enviar_sin_destinatarios_se_rechaza` | `ComunicadosTest#enviarSinDestinatariosSeRechaza` | Migrada |

### tests/roles/instructor/test_equipos.py

Java: `roles.instructor.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/instructor/test_historial_ingresos.py

Java: `roles.instructor.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#consultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/instructor/test_mensajes.py

Java: `roles.instructor.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/instructor/test_perfil.py

Java: `roles.instructor.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/instructor/test_restringidas.py

Java: `roles.instructor.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, ambientes, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/instructor/test_tutorial.py

Java: `roles.instructor.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/subdirector/test_ambientes.py

Java: `roles.subdirector.AmbientesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_ambientes` | `AmbientesTest#entraAAmbientes` | Migrada |
| `test_ve_el_detalle_de_un_ambiente` | `AmbientesTest#veElDetalleDeUnAmbiente` | Migrada |

### tests/roles/subdirector/test_ayuda.py

Java: `roles.subdirector.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/subdirector/test_carnet.py

Java: `roles.subdirector.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/subdirector/test_cerrar_sesion.py

Java: `roles.subdirector.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/subdirector/test_equipos.py

Java: `roles.subdirector.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registra_un_equipo` | `EquiposTest#registraUnEquipo` | Migrada |
| `test_elimina_su_equipo` | `EquiposTest#eliminaSuEquipo` | Migrada |

### tests/roles/subdirector/test_historial_ingresos.py

Java: `roles.subdirector.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_no_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#noConsultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/subdirector/test_mensajes.py

Java: `roles.subdirector.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/subdirector/test_perfil.py

Java: `roles.subdirector.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/subdirector/test_restringidas.py

Java: `roles.subdirector.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, asistencia, comunicados, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |

### tests/roles/subdirector/test_tutorial.py

Java: `roles.subdirector.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/roles/trabajador/test_ayuda.py

Java: `roles.trabajador.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_centro_de_ayuda` | `AyudaTest#entraAlCentroDeAyuda` | Migrada |
| `test_contacta_a_un_asesor` | `AyudaTest#contactaAUnAsesor` | Migrada |

### tests/roles/trabajador/test_carnet.py

Java: `roles.trabajador.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_el_carnet_muestra_su_perfil` | `CarnetTest#elCarnetMuestraSuPerfil` | Migrada |
| `test_el_carnet_muestra_su_documento` | `CarnetTest#elCarnetMuestraSuDocumento` | Migrada |

### tests/roles/trabajador/test_cerrar_sesion.py

Java: `roles.trabajador.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cierra_sesion` | `CerrarSesionTest#cierraSesion` | Migrada |
| `test_despues_de_salir_no_entra_al_perfil` | `CerrarSesionTest#despuesDeSalirNoEntraAlPerfil` | Migrada |

### tests/roles/trabajador/test_historial_ingresos.py

Java: `roles.trabajador.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_historial` | `HistorialIngresosTest#entraASuHistorial` | Migrada |
| `test_consulta_su_propio_historial` | `HistorialIngresosTest#consultaSuPropioHistorial` | Migrada |
| `test_no_consulta_el_historial_de_otra_persona` | `HistorialIngresosTest#noConsultaElHistorialDeOtraPersona` | Migrada |

### tests/roles/trabajador/test_mensajes.py

Java: `roles.trabajador.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_sus_mensajes` | `MensajesTest#entraASusMensajes` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_vacio_no_se_envia` | `MensajesTest#mensajeVacioNoSeEnvia` | Migrada |

### tests/roles/trabajador/test_perfil.py

Java: `roles.trabajador.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_a_su_perfil` | `PerfilTest#entraASuPerfil` | Migrada |
| `test_actualiza_su_tipo_de_sangre` | `PerfilTest#actualizaSuTipoDeSangre` | Migrada |
| `test_tipo_de_sangre_invalido_se_rechaza` | `PerfilTest#tipoDeSangreInvalidoSeRechaza` | Migrada |

### tests/roles/trabajador/test_restringidas.py

Java: `roles.trabajador.RestringidasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_no_entra` (casos migrados: escaner, gestion_usuarios, panel, pases, reportes, revision_fotos, ambientes, asistencia, comunicados, fichas, historial_clases, bandeja_mensajes; programados: historial_cambios y respaldos (fase 5)) | `RestringidasTest#noEntra` | Migrada |
| `test_no_registra_equipos` | `RestringidasTest#noRegistraEquipos` | Migrada |

### tests/roles/trabajador/test_tutorial.py

Java: `roles.trabajador.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_entra_al_tutorial` | `TutorialTest#entraAlTutorial` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/vistas/ambientes/test_ambientes.py

Java: `vistas.ambientes.AmbientesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `AmbientesTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `AmbientesTest#sinSesionPideLogin` | Migrada |
| `test_muestra_la_ficha_con_aprendices_adentro` | `AmbientesTest#muestraLaFichaConAprendicesAdentro` | Migrada |
| `test_detalle_de_una_ficha` | `AmbientesTest#detalleDeUnaFicha` | Migrada |

### tests/vistas/asistencia/test_asistencia.py

Java: `vistas.asistencia.AsistenciaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `AsistenciaTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `AsistenciaTest#sinSesionPideLogin` | Migrada |
| `test_guarda_la_asistencia_de_la_ficha` | `AsistenciaTest#guardaLaAsistenciaDeLaFicha` | Migrada |

### tests/vistas/ayuda/test_ayuda.py

Java: `vistas.ayuda.AyudaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_todos_los_perfiles_entran` (casos: 12 perfiles) | `AyudaTest#todosLosPerfilesEntran` | Migrada |
| `test_sin_sesion_pide_login` | `AyudaTest#sinSesionPideLogin` | Migrada |
| `test_contacto_con_asunto_llega_a_mensajes` | `AyudaTest#contactoConAsuntoLlegaAMensajes` | Migrada |
| `test_contacto_vacio_no_se_envia` | `AyudaTest#contactoVacioNoSeEnvia` | Migrada |

### tests/vistas/bandeja_mensajes/test_bandeja_mensajes.py

Java: `vistas.bandeja.BandejaMensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `BandejaMensajesTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `BandejaMensajesTest#sinSesionPideLogin` | Migrada |
| `test_responder_sin_permiso_devuelve_403` | `BandejaMensajesTest#responderSinPermisoDevuelve403` | Migrada |
| `test_responder_a_quien_no_existe_devuelve_404` | `BandejaMensajesTest#responderAQuienNoExisteDevuelve404` | Migrada |
| `test_el_documento_se_ve_enmascarado` | `BandejaMensajesTest#elDocumentoSeVeEnmascarado` | Migrada |

### tests/vistas/cambio_contrasena/test_cambio_contrasena.py

Java: `vistas.contrasena.CambioContrasenaTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_sin_contrasena_temporal_no_entra` | `CambioContrasenaTest#sinContrasenaTemporalNoEntra` | Migrada |
| `test_cambia_la_contrasena_temporal` | `CambioContrasenaTest#cambiaLaContrasenaTemporal` | Migrada |
| `test_exige_la_contrasena_actual` | `CambioContrasenaTest#exigeLaContrasenaActual` | Migrada |
| `test_la_nueva_debe_ser_distinta` | `CambioContrasenaTest#laNuevaDebeSerDistinta` | Migrada |

### tests/vistas/carnet/test_carnet.py

Java: `vistas.carnet.CarnetTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cada_perfil_ve_su_carnet` (casos: 12 perfiles) | `CarnetTest#cadaPerfilVeSuCarnet` | Migrada |
| `test_con_perfil_completo_muestra_codigo_de_barras` | `CarnetTest#conPerfilCompletoMuestraCodigoDeBarras` | Migrada |
| `test_con_perfil_incompleto_no_muestra_codigo_de_barras` | `CarnetTest#conPerfilIncompletoNoMuestraCodigoDeBarras` | Migrada |

### tests/vistas/cerrar_sesion/test_cerrar_sesion.py

Java: `vistas.sesion.CerrarSesionTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_todos_los_perfiles_cierran_sesion` (casos: 12 perfiles) | `CerrarSesionTest#todosLosPerfilesCierranSesion` | Migrada |
| `test_borra_el_token_de_sesion` | `CerrarSesionTest#borraElTokenDeSesion` | Migrada |
| `test_sin_sesion_pide_login` | `CerrarSesionTest#sinSesionPideLogin` | Migrada |

### tests/vistas/comunicados/test_comunicados.py

Java: `vistas.comunicados.ComunicadosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `ComunicadosTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `ComunicadosTest#sinSesionPideLogin` | Migrada |
| `test_enviar_sin_permiso_devuelve_403` | `ComunicadosTest#enviarSinPermisoDevuelve403` | Migrada |
| `test_enviar_sin_destinatarios_se_rechaza` | `ComunicadosTest#enviarSinDestinatariosSeRechaza` | Migrada |

### tests/vistas/equipos/test_equipos.py

Java: `vistas.equipos.EquiposTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_registro_segun_el_perfil` (casos: 12 perfiles) | `EquiposTest#registroSegunElPerfil` | Migrada |
| `test_maximo_cinco_equipos` | `EquiposTest#maximoCincoEquipos` | Migrada |
| `test_serial_repetido_se_rechaza` | `EquiposTest#serialRepetidoSeRechaza` | Migrada |
| `test_tipo_invalido_se_rechaza` | `EquiposTest#tipoInvalidoSeRechaza` | Migrada |
| `test_no_borra_el_equipo_de_otro` | `EquiposTest#noBorraElEquipoDeOtro` | Migrada |

### tests/vistas/escaner/test_escaner.py

Java: `vistas.escaner.EscanerTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `EscanerTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `EscanerTest#sinSesionPideLogin` | Migrada |
| `test_documento_inexistente_no_se_encuentra` | `EscanerTest#documentoInexistenteNoSeEncuentra` | Migrada |
| `test_verificar_sin_permiso_devuelve_403` | `EscanerTest#verificarSinPermisoDevuelve403` | Migrada |
| `test_registra_entrada_y_salida` | `EscanerTest#registraEntradaYSalida` | Migrada |
| `test_registra_un_incidente` | `EscanerTest#registraUnIncidente` | Migrada |

### tests/vistas/fichas/test_fichas.py

Java: `vistas.fichas.FichasTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `FichasTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `FichasTest#sinSesionPideLogin` | Migrada |
| `test_crea_edita_y_archiva` | `FichasTest#creaEditaYArchiva` | Migrada |
| `test_numero_invalido_no_se_crea` | `FichasTest#numeroInvalidoNoSeCrea` | Migrada |
| `test_ficha_repetida_no_se_duplica` | `FichasTest#fichaRepetidaNoSeDuplica` | Migrada |

### tests/vistas/gestion_usuarios/test_gestion_usuarios.py

Java: `vistas.usuarios.GestionUsuariosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `GestionUsuariosTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `GestionUsuariosTest#sinSesionPideLogin` | Migrada |
| `test_crea_un_usuario_con_contrasena_temporal` | `GestionUsuariosTest#creaUnUsuarioConContrasenaTemporal` | Migrada |
| `test_cargo_invalido_se_rechaza` | `GestionUsuariosTest#cargoInvalidoSeRechaza` | Migrada |
| `test_edita_y_desbloquea` | `GestionUsuariosTest#editaYDesbloquea` | Migrada |
| `test_elimina_un_usuario` | `GestionUsuariosTest#eliminaUnUsuario` | Migrada |
| `test_no_elimina_otro_admin` | `GestionUsuariosTest#noEliminaOtroAdmin` | Migrada |
| `test_api_sin_permiso_devuelve_403` | `GestionUsuariosTest#apiSinPermisoDevuelve403` | Migrada |

### tests/vistas/historial_cambios/test_historial_cambios.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | — | Programada fase 5 |
| `test_sin_sesion_pide_login` | — | Programada fase 5 |
| `test_muestra_los_cambios_registrados` | — | Programada fase 5 |

### tests/vistas/historial_clases/test_historial_clases.py

Java: `vistas.clases.HistorialClasesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `HistorialClasesTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `HistorialClasesTest#sinSesionPideLogin` | Migrada |
| `test_busca_por_ficha` | `HistorialClasesTest#buscaPorFicha` | Migrada |

### tests/vistas/historial_ingresos/test_historial_ingresos.py

Java: `vistas.ingresos.HistorialIngresosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_todos_los_perfiles_entran` (casos: 12 perfiles) | `HistorialIngresosTest#todosLosPerfilesEntran` | Migrada |
| `test_sin_sesion_pide_login` | `HistorialIngresosTest#sinSesionPideLogin` | Migrada |
| `test_historial_de_otra_persona_segun_el_perfil` (casos: 12 perfiles) | `HistorialIngresosTest#historialDeOtraPersonaSegunElPerfil` | Migrada |
| `test_su_propio_historial` | `HistorialIngresosTest#suPropioHistorial` | Migrada |

### tests/vistas/login/test_login.py

Java: `vistas.login.LoginTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_cubre_todos_los_cargos` | `LoginTest#cubreTodosLosCargos` | Migrada |
| `test_entra_con_correo` (casos: 11 usuarios, uno por cargo) | `LoginTest#entraConCorreo` | Migrada |
| `test_entra_con_documento` (casos: 11 usuarios, uno por cargo) | `LoginTest#entraConDocumento` | Migrada |
| `test_correo_sin_importar_mayusculas_ni_espacios` (casos: 11 usuarios, uno por cargo) | `LoginTest#correoSinImportarMayusculasNiEspacios` | Migrada |
| `test_formulario_sin_ajax_redirige` (casos: 11 usuarios, uno por cargo) | `LoginTest#formularioSinAjaxRedirige` | Migrada |
| `test_ya_autenticado_no_vuelve_a_ver_el_login` (casos: 11 usuarios, uno por cargo) | `LoginTest#yaAutenticadoNoVuelveAVerElLogin` | Migrada |
| `test_contrasena_incorrecta_no_entra` (casos: 11 usuarios, uno por cargo) | `LoginTest#contrasenaIncorrectaNoEntra` | Migrada |
| `test_bloqueo_tras_cinco_intentos` (casos: 11 usuarios, uno por cargo) | `LoginTest#bloqueoTrasCincoIntentos` | Migrada |
| `test_no_revela_si_la_cuenta_existe` | `LoginTest#noRevelaSiLaCuentaExiste` | Migrada |
| `test_el_contador_se_reinicia_al_acertar` | `LoginTest#elContadorSeReiniciaAlAcertar` | Migrada |
| `test_campos_vacios_no_entran` (casos: sin-contrasena, sin-identificador, vacio) | `LoginTest#camposVaciosNoEntran` | Migrada |
| `test_la_pagina_de_login_abre` | `LoginTest#laPaginaDeLoginAbre` | Migrada |

### tests/vistas/mensajes/test_mensajes.py

Java: `vistas.mensajes.MensajesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_todos_los_perfiles_entran` (casos: 12 perfiles) | `MensajesTest#todosLosPerfilesEntran` | Migrada |
| `test_sin_sesion_pide_login` | `MensajesTest#sinSesionPideLogin` | Migrada |
| `test_envia_un_mensaje` | `MensajesTest#enviaUnMensaje` | Migrada |
| `test_mensaje_demasiado_largo_no_se_envia` | `MensajesTest#mensajeDemasiadoLargoNoSeEnvia` | Migrada |
| `test_solo_ve_sus_mensajes` | `MensajesTest#soloVeSusMensajes` | Migrada |

### tests/vistas/panel/test_panel.py

Java: `vistas.panel.PanelTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `PanelTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `PanelTest#sinSesionPideLogin` | Migrada |
| `test_exportar_sin_fechas_no_descarga` | `PanelTest#exportarSinFechasNoDescarga` | Migrada |
| `test_exportar_con_fechas_descarga_csv` | `PanelTest#exportarConFechasDescargaCsv` | Migrada |
| `test_exportar_sin_permiso_no_descarga` | `PanelTest#exportarSinPermisoNoDescarga` | Migrada |

### tests/vistas/pases/test_pases.py

Java: `vistas.pases.PasesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `PasesTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `PasesTest#sinSesionPideLogin` | Migrada |
| `test_visitante_sin_documento_no_se_crea` | `PasesTest#visitanteSinDocumentoNoSeCrea` | Migrada |
| `test_crea_y_desactiva_un_objeto` | `PasesTest#creaYDesactivaUnObjeto` | Migrada |
| `test_crear_sin_permiso_devuelve_403` | `PasesTest#crearSinPermisoDevuelve403` | Migrada |

### tests/vistas/perfil/test_perfil.py

Java: `vistas.perfil.PerfilTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_todos_los_perfiles_entran` (casos: 12 perfiles) | `PerfilTest#todosLosPerfilesEntran` | Migrada |
| `test_sin_sesion_pide_login` | `PerfilTest#sinSesionPideLogin` | Migrada |
| `test_muestra_su_nombre` (casos: 12 perfiles) | `PerfilTest#muestraSuNombre` | Migrada |
| `test_documento_de_otra_persona_se_rechaza` | `PerfilTest#documentoDeOtraPersonaSeRechaza` | Migrada |
| `test_nombres_con_numeros_se_rechazan` | `PerfilTest#nombresConNumerosSeRechazan` | Migrada |

### tests/vistas/politica_privacidad/test_politica_privacidad.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_abre_sin_sesion` | — | Programada fase 5 |
| `test_abre_con_sesion` (casos: 12 perfiles) | — | Programada fase 5 |

### tests/vistas/recuperacion/test_recuperacion.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_la_pagina_abre` | — | Programada fase 5 |
| `test_no_se_salta_al_paso_de_verificar` | — | Programada fase 5 |
| `test_no_se_salta_al_paso_de_cambiar` | — | Programada fase 5 |
| `test_cualquier_perfil_pide_codigo` (casos: 12 perfiles) | — | Programada fase 5 |
| `test_cambia_la_contrasena_con_el_codigo` | — | Programada fase 5 |

### tests/vistas/registro/test_registro.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_la_pagina_abre` | — | Programada fase 5 |
| `test_se_registra_como_aprendiz` | — | Programada fase 5 |
| `test_no_puede_elegir_otro_cargo` | — | Programada fase 5 |
| `test_contrasenas_distintas_no_registran` | — | Programada fase 5 |
| `test_correo_repetido_no_registra` | — | Programada fase 5 |

### tests/vistas/reportes/test_reportes.py

Java: `vistas.reportes.ReportesTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `ReportesTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `ReportesTest#sinSesionPideLogin` | Migrada |
| `test_cada_reporte_abre` (casos: Aprendiz, Instructor, Personal) | `ReportesTest#cadaReporteAbre` | Migrada |

### tests/vistas/respaldos/test_respaldos.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | — | Programada fase 5 |
| `test_sin_sesion_pide_login` | — | Programada fase 5 |
| `test_descargar_sin_permiso_no_descarga` | — | Programada fase 5 |
| `test_no_descarga_fuera_de_la_carpeta` | — | Programada fase 5 |

### tests/vistas/revision_fotos/test_revision_fotos.py

Java: `vistas.fotos.RevisionFotosTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_acceso_segun_el_perfil` (casos: 12 perfiles) | `RevisionFotosTest#accesoSegunElPerfil` | Migrada |
| `test_sin_sesion_pide_login` | `RevisionFotosTest#sinSesionPideLogin` | Migrada |
| `test_revisar_sin_permiso_devuelve_403` | `RevisionFotosTest#revisarSinPermisoDevuelve403` | Migrada |
| `test_rechazar_sin_motivo_se_rechaza` | `RevisionFotosTest#rechazarSinMotivoSeRechaza` | Migrada |
| `test_aprueba_una_foto` | `RevisionFotosTest#apruebaUnaFoto` | Migrada |

### tests/vistas/tutorial/test_tutorial.py

Java: `vistas.tutorial.TutorialTest`

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_todos_los_perfiles_entran` (casos: 12 perfiles) | `TutorialTest#todosLosPerfilesEntran` | Migrada |
| `test_sin_sesion_pide_login` | `TutorialTest#sinSesionPideLogin` | Migrada |
| `test_marca_el_tutorial_como_visto` | `TutorialTest#marcaElTutorialComoVisto` | Migrada |

### tests/vistas/verificacion/test_verificacion.py

Java: sin clase todavía

| Función pytest | Clase#método Java | Estado |
|---|---|---|
| `test_ya_verificado_no_entra` | — | Programada fase 5 |
| `test_verifica_con_el_codigo_correcto` | — | Programada fase 5 |
| `test_codigo_incorrecto_no_verifica` | — | Programada fase 5 |
| `test_codigo_expirado_no_verifica` | — | Programada fase 5 |

## Resumen

| Estado | Funciones pytest |
|---|---|
| Migrada | 695 |
| Programada fase 5 | 98 |
| Descartada | 11 |
| **Total** | **804** |

Portería 2 tiene 804 funciones `test_` en 174 archivos; todas aparecen arriba.
En `test_restringidas.py` de cada perfil la función está migrada con los casos cuyas pantallas ya existen; los casos de pantallas de fases futuras se indican en la misma fila.
