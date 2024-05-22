USE AplicativoMovil
GO
/**** Creamos procedimientos en la base de datos *****/

-- 1. Para la validación del Código Modular
CREATE PROCEDURE dbo.ValidaCodigo
 @CodMod char(7) = null,
 @Anexo char(1) = null
AS
	Select SE.COD_MOD, SE.ANEXO, 
			CEN_EDU = SE.CEN_EDU + ' (' + isnull(NE.NombreNivel,'') + ')', 
			SE.ESTADO 
	from ServicioEducativo SE
	left join NivelEducativo NE on (SE.NIV_MOD = NE.CodNivel)
	where SE.COD_MOD = @CodMod and SE.ANEXO = @Anexo 

GO

-- 2. Para el registro de las coordenadas enviadas por la app

CREATE PROCEDURE dbo.RegistraDatosMovil
 @CodMod char(7),
 @Anexo char(1),
 @Latitud varchar(250) = null,
 @Longitud varchar(250) = null,
 @Precision varchar(250) = null,
 @Altitud varchar(250) = null,
 @FotoPath varchar(150) = null,
 @Fecha varchar(100) = null
AS

	If (@CodMod is null)
	Begin
		Select @CodMod = '999999', @Anexo = '9'--, @FotoPath = ''
	End 

	Insert into dbo.CoordenadasMovil (COD_MOD, ANEXO, LATITUD, LONGITUD, [PRECISION], ALTITUD, FOTO_PATH, FECHA)
	values (
		@CodMod,
		@Anexo,
		@Latitud, 
		@Longitud,
		@Precision,
		@Altitud,
		/*
		IIF( ISNUMERIC(@Latitud) = 1, @Latitud, null),
		IIF( ISNUMERIC(@Longitud) = 1, @Longitud, null),
		IIF( ISNUMERIC(@Precision) = 1, @Precision, null),
		*/
		@FotoPath,
		@Fecha
	)

GO