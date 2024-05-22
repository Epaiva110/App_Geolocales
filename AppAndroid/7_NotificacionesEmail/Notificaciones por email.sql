USE PADRONSIGMED
GO

/*

-- CREACIÓN DE TABLAS PARA EL ENVÍO DE LAS NOTIFICACIONES A LOS ESTADÍSTICOS

Create Table dbo.NotificacionesMovil (
	RowId int identity(1,1) not null, 
	FECHA_REG varchar(50) null,
	DRE_UGEL varchar(150) null, 
	CODOOII char(6) null, 
	CODLOCAL char(7) null, 
	COD_MOD char(7) null, 
	NIV_MOD char(2) null, 
	CEN_EDU_P varchar(100) null, 
	CEN_EDU_M varchar(100) null, 
	LATITUD numeric(38,8) null,
	LONGITUD numeric(38,8) null,
	ALTITUD numeric(38,8) null,
	[PRECISION] numeric(38,8) null,
	IDDIST char(6) null,
	CODGEO char(6) null,
	CODCP_MED char(6) null,
	CEN_POB varchar(100) null, 
	GES_DEP char(2) null, 
	REV_FOTOS int null, 
	Dist_CODGEO numeric(38,8) null,
	Dist_CP numeric(38,8) null,
	Dist_IE numeric(38,8) null,
	Cat_CODGEO int null, 
	EMPATE_PAD varchar(10) null, 
	FOTOS varchar(10) null, 
	OBSERVACIONES varchar(250) null, 
	OBS_CODGEO varchar(10) null, 
	OBS_CP varchar(10) null, 
	OBS_IE varchar(10) null, 
	Cat_Foto varchar(250) null, 
	OBS_Revfot varchar(250) null, 
	Exp1 int null, 
	Exp2 int null, 
	Exp3 int null, 
	Consist_CODGEO varchar(250) null, 
	Consist_IS int null, 
	FTECOORDENADAS int null
)


	Drop table NotificacionesMovil

-- @Importamos Excel



Create table dbo.EstadisticosUGEL (
	CODUGEL char(6) not null, 
	NOMBRE_UGEL varchar(100) null, 
	APELLIDOS varchar(100) null, 
	NOMBRES varchar(100) null, 
	DNI varchar(10) null, 
	CELULAR varchar(20) null, 
	EMAIL varchar(100) null 
--Constraint PK_EstadisticosUGEL Primary Key Clustered (CODUGEL)
)


	Drop table EstadisticosUGEL

-- @Importamos Excel



Select *				-- Marco del CE2022
into MarcoCenso2022
from ServicioEducativo

Create Clustered Index IN_MarcoCenso2022_CODMOD_01 ON dbo.MarcoCenso2022 (COD_MOD);

Drop Index IN_MarcoCenso2022_CODMOD_01 ON dbo.MarcoCenso2022;


*/


-----------------------------------


/* PROCEDIMIENTO PARA EL ENVÍO DE LAS NOTIFICACIONES POR CORREO ELECTRÓNICO */

Declare @CODUGEL as char(6), @NombreUGEL as varchar(250)
Declare @Emails as varchar(250), 
		@Asunto as varchar(100), 
		@HTMLBody as varchar(max) , 
		@ArchivoAdjunto as varchar(100)  
Declare @FechaRevision varchar(24), --varchar(10), 
		@Fecha as date, 
		@Anio as varchar(4), 
		@Mes as varchar(4), 
		@Dia as varchar(4)
Declare @HTMLTable as varchar(max)


-- Obtención de la fecha actual
Select @Fecha = getdate()

Select @Anio = year(@Fecha), 
		@Mes = '0' + cast(month(@Fecha) as varchar(4)), 
		@Dia = '0' + cast(day(@Fecha) as varchar(4))

Select @FechaRevision = right(@Dia,2) + '/' + right(@Mes,2) + '/' + @Anio 
Select @FechaRevision = '08/09/2022 (1:00 p.m.)'


-- Definición del asunto del mensaje
Set @Asunto = 'Reporte de avance de levantamiento de Latitud, Longitud y Altitud para la Cédula de Local Educativo del Censo Educativo 2022.'


-- Generación de lista de UGEL's a enviar notificación
Select distinct CODOOII 
into #TablaUGEL
from NotificacionesMovil
where CODOOII in (
	--'110001'			
	Select CODUGEL from EstadisticosUGEL		-- select distinct CODUGEL from EstadisticosUGEL		-- 247
	Group by CODUGEL	
)
order by CODOOII


While exists (Select 1 from #TablaUGEL)
Begin

		-- Selección de la UGEL a notificar
		Select top 1 @CODUGEL = CODOOII from #TablaUGEL

		
		-- Obtención de correos de UGEL
		Select @NombreUGEL = NOMBRE_UGEL, @Emails = 
		--Select @NombreUGEL = 'UGEL ICA', @Emails = 
				Stuff((
					select ';' + Replace(isnull(EMAIL,''),',',';') from EstadisticosUGEL where CODUGEL = A.CODUGEL
					for xml path('')
				),1,1,'') 
		from (
			Select CODUGEL, NOMBRE_UGEL from EstadisticosUGEL		-- select distinct CODUGEL from EstadisticosUGEL		-- 247
			Group by CODUGEL, NOMBRE_UGEL 
		) A
		--where CODUGEL = '110000'
		where CODUGEL = @CODUGEL


		-- Generación de la tabla de resultados por UGEL
		/*
		Set @HTMLTable = '<table border=1 cellspacing=0 cellpadding=0>' +
			'<tr><th>Fecha de Registro</th><th>Código Modular</th><th>Nombre del Servicio</th><th>Observación</th></tr>' + 
			Cast( (

					Select td = left(B.FECHA_REG,10), '', td = COD_MOD, '', td = CEN_EDU, '',
							--Consist_CODGEO, FTECOORDENADAS, Consist_LE, Consist_IS, A.Consist_Foto,
							td = case when (OBS_COORDS = 'Pendiente de Revisión' or OBS_FOTOS = 'Pendiente de Revisión') then 'Coordenadas recibidas. Pendiente de Revisión.' else OBS_COORDS + ' ' + OBS_FOTOS end, ''
							--td = OBS_COORDS + ' ' + OBS_FOTOS, ''
							--td = case when (OBS_Revfot is null) then OBS_COORDS + ' ' + OBS_FOTOS else OBS_COORDS + ' ' + OBS_FOTOS + ' ' + OBS_Revfot end , ''
					from (

						Select A.FECHA_REG, A.COD_MOD, A.CODOOII, MC.CEN_EDU,
								A.Consist_CODGEO, A.FTECOORDENADAS, A.Consist_LE, A.Consist_IS, A.Consist_Foto,
								OBS_COORDS = 
									Case when (Consist_IS = -1 or Consist_Foto = -1)  then 'Pendiente de Revisión'
									else
										Case 
										when (Consist_CODGEO = 1 and ((FTECOORDENADAS = 2 and Consist_LE = 1) or FTECOORDENADAS = 1) and Consist_IS <> 3 and Consist_Foto <> 5)
													then 'Coordenadas válidas para el CE.' 
										when ((Consist_CODGEO = 2 or (Consist_CODGEO = 1 and Consist_LE = 2 and FTECOORDENADAS = 2)) and Consist_IS <> 3 and Consist_Foto <> 5)
													then 'Coordenadas válidas para el CE, aunque no coinciden con el Padrón de IIEE.' 
										when (Consist_CODGEO = 3 and Consist_LE = 1 and Consist_IS <> 3 and Consist_Foto <> 5)
													then 'Coordenadas válidas para el CE por coincidencia con el Padrón de IIEE, aunque se posicionan fuera de la región.' 
										when ((Consist_CODGEO = 3 and Consist_LE = 2) or Consist_IS = 3 or Consist_Foto = 5)
													then 'Coordenadas no válidas para el CE. Se posicionan en zonas inhabitadas o fuera de la región o no se pudo identificar el servicio.' 
										else ''
										end
									end, 
								OBS_FOTOS = 
									Case when (Consist_IS = -1 or Consist_Foto  = -1)  then 'Pendiente de Revisión'
									else
										Case 
										when Consist_Foto = 1 
													then 'Foto bien tomada.' 
										else 'Sin foto o foto mal tomada o no se identifica la IE.'
										end
									end
								--, OBS_Revfot
						from (
								Select FECHA_REG, COD_MOD, CODLOCAL, CODOOII, LATITUD, LONGITUD, 
										Consist_CODGEO = Cat_CODGEO, 
										FTECOORDENADAS,
										Consist_LE = case when Dist_IE > 5000 then 2 else 1 end,
										Consist_IS = Consist_IS,
										Consist_Foto = REV_FOTOS
										--, OBS_Revfot
								from NotificacionesMovil
								where CODOOII = @CODUGEL 
								) A
						left join ( Select COD_MOD, ANEXO, CEN_EDU from MarcoCenso2022 where ANEXO =0 ) MC on (A.COD_MOD = MC.COD_MOD)

					) B
					order by cast(substring(B.FECHA_REG,7,4) + substring(B.FECHA_REG,4,2) + substring(B.FECHA_REG,1,2) as datetime)
					for xml path('tr'), type

			) as varchar(max)) + 
			'<table>'
			*/


-- Generación del cuerpo del correo a enviar
Set @HTMLBody = '
Estimado estadístico, <br><br>

Por la presente adjuntamos el reporte de la <b>'+@NombreUGEL+'</b> al <b>'+@FechaRevision+'</b>, sobre el avance del levantamiento de las coordenadas de ubicación: Latitud, Longitud y Altitud, con el aplicativo <b><i>GeoLOCALES censo</i></b>, correspondientes a la Pregunta 10 de la Cédula 11 del Censo Educativo (CE) 2022. <br><br>

Es importante señalar, que las coordenadas de ubicación levantadas con el aplicativo <b><i>GeoLOCALES censo</i></b> que no han podido considerarse válidas para el CE 2022, han sido únicamente aquellas que no cumplían con un mínimo de criterios de calidad: No se pudo identificar el servicio, la posición se ubica muy lejos de la registrada en el padrón, o se ubica en áreas inhóspitas. <br><br>

Sin embargo, adicionalmente se han identificado en el reporte los casos en los cuales las coordenadas de ubicación han sido recibidas para el CE, pero presentan algunas observaciones como: No coinciden con los datos de ubicación del Padrón de IIEE, o no se puede identificar el local educativo en la foto. Si bien estas observaciones no invalidan las coordenadas de ubicación levantadas para el CE, los reportamos de modo que puedan informar a los directores interesados, a fin de que tengan la oportunidad de realizar otra toma (voluntaria). <br><br>

Este correo ha sido generado automáticamente, no intente responder por este medio. Cualquier consulta, puede contactarnos a los siguientes correos: <br><br>

Amalia Sevilla, <a href=mailto:asevilla@minedu.gob.pe>asevilla@minedu.gob.pe</a> <br>
Francisco Solano, <a href=mailto:fsolano@minedu.gob.pe>fsolano@minedu.gob.pe</a> <br>
Julián Quispe,  <a href=mailto:julquispe@minedu.gob.pe>julquispe@minedu.gob.pe</a> <br><br>

Saludos cordiales <br><br>

Área de Análisis Territorial <br>
Unidad de Estadística <br>
Ministerio de Educación <br>
------------------------------------------------<br><br>
' --+ @HTMLTable + '<br>'


		Set @ArchivoAdjunto = 'E:\NotificacionesEmail\Adjuntos\' + @CODUGEL + '.xls'


		-- Envío de la notificación por correo electrónico
		If (@Emails <> '')
		Begin

			EXEC msdb.dbo.sp_send_dbmail
				 @profile_name = 'Notificaciones PADRONSIGMED',
				 @recipients = @Emails,									--	';jidelacruz@minedu.gob.pe',
				 @copy_recipients = 'julquispe@minedu.gob.pe',
				 @body = @HTMLBody,
				 @body_format = 'HTML',
				 @subject = @Asunto,
				 @file_attachments = @ArchivoAdjunto

		End


		-- Eliminación de la UGEL de la lista de envios
		Delete from #TablaUGEL where CODOOII = @CODUGEL 

End


-- Eliminación de la lista de envios
Drop table #TablaUGEL






/* 1er envio */

-- Datos al 02/06

Select * from NotificacionesMovil_0531	-- 73



Select * from NotificacionesMovil_0602		
-- Delete from NotificacionesMovil_0602
where COD_MOD in ('0309930','1016484')


Select * from NotificacionesMovil_0602
-- Delete from NotificacionesMovil_0602
where RowID in (
83, 53, 7, 86, 
106, 107, 108, 118, 36, 59, 60, 62, 63, 64, 65, 66, 68, 69, 149, 150, 151, 153, 154, --<> 58
76, 74, 142, 
35, 162, 163, 84, --<> 161
43, 5, 103, 145, 140, 113
)

		-- Nuevo total: 127


Insert into NotificacionesMovil (
	FECHA_REG, DRE_UGEL, CODOOII, 
	CODLOCAL, COD_MOD, NIV_MOD, 
	CEN_EDU_P, CEN_EDU_M, LATITUD,
	LONGITUD, ALTITUD, [PRECISION],
	IDDIST, CODGEO, CODCP_MED,
	CEN_POB, GES_DEP, REV_FOTOS, 
	Dist_CODGEO, Dist_CP, Dist_IE,
	Cat_CODGEO, EMPATE_PAD, FOTOS, 
	OBSERVACIONES, OBS_CODGEO, OBS_CP, 
	OBS_IE, Cat_Foto, OBS_Revfot, 
	Exp1, Exp2, Exp3, Consist_CODGEO, 
	Consist_IS, FTECOORDENADAS  
) 
Select 	FECHA_REG, DRE_UGEL, CODOOII, 
	CODLOCAL, COD_MOD, NIV_MOD, 
	CEN_EDU_P, CEN_EDU_M, LATITUD,
	LONGITUD, ALTITUD, [PRECISION],
	IDDIST, CODGEO, CODCP_MED,
	CEN_POB, GES_DEP, REV_FOTOS, 
	Dist_CODGEO, Dist_CP, Dist_IE,
	Cat_CODGEO, EMPATE_PAD, FOTOS, 
	OBSERVACIONES, OBS_CODGEO, OBS_CP, 
	OBS_IE, Cat_Foto, OBS_Revfot, 
	Exp1, Exp2, Exp3, Consist_CODGEO, 
	Consist_IS, FTECOORDENADAS  
from NotificacionesMovil_0602


Select * from NotificacionesMovil		-- 73 + 127 : 200



/* 2do envio */

-- Datos al 14/06

Truncate table dbo.NotificacionesMovil		-- 200


Select * from NotificacionesMovil			-- 835

Select OBS_Revfot, count(*) from NotificacionesMovil
group by OBS_Revfot


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD


-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL



/* 3er envio */

-- Datos al 22/06


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 903


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD


-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 4to envio */

-- Datos al 30/06

Select * into NotificacionesMovil_0622
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_2206


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 5392


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD


-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL



/* 5to envio */

-- Datos al 06/07

Select * into NotificacionesMovil_0630
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_3006


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 7646


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD


Select * from NotificacionesMovil
--	Update NotificacionesMovil set FECHA_REG = '04/07/2022  08:14:17 a. m.'
where RowId = '7646'


-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL



/* 6to envio */

-- Datos al 15/07

Select * into NotificacionesMovil_0706
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0706


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 12041


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 0
where left(FECHA_REG,2) like '%[^0-9]%'

Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (8655)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (8655)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (8655)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									-- 0274258, 2672514, 1602812


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 7to envio */

-- Datos al 20/07

Select * into NotificacionesMovil_0713
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0713


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 16757


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 1
where left(FECHA_REG,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '0'+FECHA_REG
	where RowId = 12717


Select * from NotificacionesMovil		-- 1
where substring(FECHA_REG,4,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43p. m.'
	where RowId = 10509


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 12717



Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (10178)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (4690)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (8655)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 8to envio */

-- Datos al 24/07

Select * into NotificacionesMovil_0720
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0720


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 19163


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 4
where left(FECHA_REG,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 19160

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 19161

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 19162

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:22'
	where RowId = 19163



Select * from NotificacionesMovil		-- 1
where substring(FECHA_REG,4,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43p. m.'
	where RowId = 13562


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	--where RowId = 



Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (12590)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (7102)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 9to envio */

-- Datos al 27/07 (1:00 p.m.)

Select * into NotificacionesMovil_0724
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0724		-- 19163


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 21200


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 4
where left(FECHA_REG,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 21197

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 21198

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 21199

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:22'
	where RowId = 21200



Select * from NotificacionesMovil		-- 1
where substring(FECHA_REG,4,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43p. m.'
	where RowId = 13485


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	--where RowId = 



Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (14627)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (9141)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 10mo envio */

-- Datos al 03/08 (1:00 p.m.)

Select * into NotificacionesMovil_0727
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0727		-- 21200


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 23710


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 4
where left(FECHA_REG,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 23707

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 23708

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 23709

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:22'
	where RowId = 23710



Select * from NotificacionesMovil		-- 1
where substring(FECHA_REG,4,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43p. m.'
	where RowId = 14744


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (17137)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (11651)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 11mo envio */

-- Datos al 05/08 (1:00 p.m.)

Select * into NotificacionesMovil_0803
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0803		-- 23710


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 24591


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 23470

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 24587
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 24588

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 24589

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 24590

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:12:48'
	where RowId = 24591



Select * from NotificacionesMovil		-- 1
where substring(FECHA_REG,4,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 15661


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (18018)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (12532)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 12vo envio */

-- Datos al 10/08 (1:00 p.m.)

Select * into NotificacionesMovil_0805
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0805		-- 24591


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 27002


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 25884

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 26998
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 26999

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 27000

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 27001

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 27002



Select * from NotificacionesMovil		-- 1
where substring(FECHA_REG,4,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 18134


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (20429)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (14944)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 13vo envio */

-- Datos al 12/08 (1:00 p.m.)

Select * into NotificacionesMovil_0810
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0810		-- 27002


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 27002


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 27487

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 28596
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 28597

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 28598

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 28599

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:20'
	where RowId = 28600



Select * from NotificacionesMovil		-- 1
where substring(FECHA_REG,4,2) like '%[^0-9]%'

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 19760


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (22027)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (16542)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 7
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 14vo envio */

-- Datos al 17/08 (1:00 p.m.)

Select * into NotificacionesMovil_0812
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0812		-- 28600


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 30352


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 5
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 29243

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 30349

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 30350

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 30351

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 30352



Select * from NotificacionesMovil		-- 2
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/08/2022 4:08:55 p.m.'
	where RowId = 16759	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 21545


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (23779)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (18294)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 17243
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 15vo envio */

-- Datos al 19/08 (1:00 p.m.)

Select * into NotificacionesMovil_0817
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0817		-- 30352


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 31357


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 5
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 30250

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 31354

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 31355

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 31356

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 31357



Select * from NotificacionesMovil		-- 2
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 18277	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 22567


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (24784)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (19299)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 18248
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 16vo envio */

-- Datos al 22/08 (1:00 p.m.)

Select * into NotificacionesMovil_0819
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0819		-- 31357


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 31961


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 5
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 30855

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 31958

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 31959

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 31960

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:22'
	where RowId = 31961



Select * from NotificacionesMovil		-- 2
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 10359	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 18266	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 22773


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (25388)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (19903)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 18852
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 17vo envio */

-- Datos al 23/08 (3:45 p.m.)

Select * into NotificacionesMovil_0822
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0822		-- 31961


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 32538


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 5
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 31432

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 32535

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 32536

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 32537

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 32538



Select * from NotificacionesMovil		-- 2
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 10346	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 18234	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 22737


Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (25965)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (20480)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 19429
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL




/* 18vo envio */

-- Datos al 24/08 (4:00 p.m.)

Select * into NotificacionesMovil_0823
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0823		-- 32538


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 33479


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 5
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 32373

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 33476

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 33477

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 33478

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:20'
	where RowId = 33479



Select * from NotificacionesMovil		-- 2
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 10323	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 22691

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/08/2022 4:11:58 p.m.'
	where RowId = 27075	




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (26906)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (21421)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 20370
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 19vo envio */

-- Datos al 26/08 (1:00 p.m.)

Select * into NotificacionesMovil_0824
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0824		-- 33479


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 34710


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 5
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 33606

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '05/08/2022 12:41:41 p.m.'
	where RowId = 
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 34707

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 34708

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 34709

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:48'
	where RowId = 34710



Select * from NotificacionesMovil		-- 2
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 10291	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 18159	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 22637




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (26906)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (21421)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 2
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 20370
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 20vo envio */

-- Datos al 31/08 (1:00 p.m.)

Select * into NotificacionesMovil_0826
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0826		-- 34710


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 35768

		/*
		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where Right(FECHA_REG,4) like '%[^0-9a-z.]'


		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where CODMOD = '1763176'
		*/


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 5
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 34185
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 35764

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 35765

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 35766

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 35767

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '31/08/2022 10:10:27'
	where RowId = 35768



Select * from NotificacionesMovil		-- 2
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 10264	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 18109	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 22581




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (29195)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (23710)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (312)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 22659
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL






/* 21vo envio */

-- Datos al 01/09 (1:00 p.m.)

Select * into NotificacionesMovil_0831
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0831		-- 35768


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 36412

		/*
		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where Right(FECHA_REG,4) like '%[^0-9a-z.]'


		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where CODMOD = '1763176'
		*/


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 34568
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 36408

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 36409

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 36410

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 36411

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '31/08/2022 10:10:27'
	where RowId = 36412



Select * from NotificacionesMovil		-- 3
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 10679	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 18520	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 22985




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (29839)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (24354)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (859)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 23303
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 22vo envio */

-- Datos al 02/09 (4:00 p.m.)

Select * into NotificacionesMovil_0901
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0901		-- 36412


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 37202

		/*
		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where Right(FECHA_REG,4) like '%[^0-9a-z.]'


		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where CODMOD = '1763176'
		*/


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 35366
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 37198

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 37199

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 37200

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 37201

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '31/08/2022 10:10:27'
	where RowId = 37202



Select * from NotificacionesMovil		-- 3
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 11492	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/08/2022 4:11:58 p.m.'
	where RowId = 28151	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 23787




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (30629)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (25144)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (1532)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6536

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 24093
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL






/* 23vo envio */

-- Datos al 05/09 (3:00 p.m.)

Select * into NotificacionesMovil_0902
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0902		-- 37202


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 38657

		/*
		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where Right(FECHA_REG,4) like '%[^0-9a-z.]'


		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where CODMOD = '1763176'
		*/


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 36810
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 38653

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 38654

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 38655

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:17'
	where RowId = 38656

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '31/08/2022 10:10:27'
	where RowId = 38657



Select * from NotificacionesMovil		-- 3
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 12643	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/08/2022 4:12:05 p.m.'
	where RowId = 29513	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 25085




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (32084)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (26600)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6535

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 25549
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 24vo envio */

-- Datos al 06/09 (4:00 p.m.)

Select * into NotificacionesMovil_0905
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0905		-- 38657


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 39498

		/*
		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where Right(FECHA_REG,4) like '%[^0-9a-z.]'


		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where CODMOD = '1763176'
		*/


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD



Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 37656
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 39494

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 39495

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 39496

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:20'
	where RowId = 39497

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '31/08/2022 10:10:27'
	where RowId = 39498



Select * from NotificacionesMovil		-- 3
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 13529	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 21408	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 25946




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (32926)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (27442)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (2)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6535

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 26391
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL





/* 25vo envio */

-- Datos al 07/09 (4:00 p.m.)

Select * into NotificacionesMovil_0906
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0906		-- 39498


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 40536

		/*
		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where Right(FECHA_REG,4) like '%[^0-9a-z.]'


		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where CODMOD = '1763176'
		*/


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD


		Select * from NotificacionesMovil
		-- Delete from NotificacionesMovil
		where RowId = 40527


Select * from NotificacionesMovil		-- 6
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 38660
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 40494

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 40495

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 40496

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:20'
	where RowId = 40497

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '07/09/2022 3:11:36 p.m.'
	where RowId = 40498

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '31/08/2022 10:10:27'
	where RowId = 40499


Select * from NotificacionesMovil		-- 3
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 14550	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '17/08/2022 7:03:41 p.m.'
	where RowId = 22436	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 26967




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (33963)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (28479)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (38)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 0
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6535

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 27428
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL






/* 25vo envio */

-- Datos al 08/09 (1:00 p.m.)

Select * into NotificacionesMovil_0907
from NotificacionesMovil		

	-- Select * from NotificacionesMovil_0907		-- 40535


Truncate table dbo.NotificacionesMovil		


Select * from NotificacionesMovil			-- 41274 (-1) (-16) : 41257

		/*
		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where Right(FECHA_REG,4) like '%[^0-9a-z.]'


		Select * from TBL_REPORTE_ESTADISTICOS_FINAL
		where CODMOD = '1763176'
		*/


Select * from NotificacionesMovil			-- 0
where COD_MOD in (

	Select COD_MOD
			--, count(*) 
	from NotificacionesMovil
	group by COD_MOD
	having count(*) > 1

)
order by COD_MOD


		Select * from NotificacionesMovil
		-- Delete from NotificacionesMovil
		where RowId = 13


Select * from NotificacionesMovil		-- 7
where left(FECHA_REG,2) like '%[^0-9]%'

	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '03/08/2022 6:01:07 p.m.'
	where RowId = 39422
	   	 
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '15/06/2022 9:05:07'
	where RowId = 41253

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/06/2022 11:42:18'
	where RowId = 41254

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '24/06/2022 13:02:22'
	where RowId = 41255

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '04/07/2022 8:14:22'
	where RowId = 41256

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '07/09/2022 3:11:36 p.m.'
	where RowId = 41257

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '31/08/2022 10:10:27'
	where RowId = 41258


Select * from NotificacionesMovil		-- 3
where substring(FECHA_REG,4,2) like '%[^0-9]%'
	
	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '11/08/2022 8:26:00 a.m.'
	where RowId = 15322	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '23/08/2022 4:11:58 p.m.'
	where RowId = 32148	

	Select * from NotificacionesMovil
	-- Update NotificacionesMovil set FECHA_REG = '20/07/2022 4:00:43 p.m.'
	where RowId = 27731




Select * from NotificacionesMovil
where substring(FECHA_REG,4,2) > 12


Select REV_FOTOS, count(*) from NotificacionesMovil		-- -1 (34701)
group by REV_FOTOS 
order by REV_FOTOS 

Select Consist_IS, count(*) from NotificacionesMovil	-- -1 (29217)
group by Consist_IS 
order by Consist_IS 


Select Cat_CODGEO, count(*) from NotificacionesMovil	-- NULL (154)
group by Cat_CODGEO 
order by Cat_CODGEO 

Select * from NotificacionesMovil						-- 152
where Dist_IE is null									


Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS <> Consist_IS		-- 6535

Select * from NotificacionesMovil
where REV_FOTOS = -1 and REV_FOTOS = Consist_IS			-- 28166
		and Cat_CODGEO is null

Select * from NotificacionesMovil						-- 0 
where REV_FOTOS <> -1 
		and Cat_CODGEO is null



-- Se enviaron las notificaciones de la UGEL ICA a la DRE ICA

Select *
from EstadisticosUGEL
where CODUGEL like '1100%'

-- Caso CODOOII: 110001 que no se encuentran en la tabla EstadisticosUGEL

