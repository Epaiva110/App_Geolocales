USE AplicativoMovil
GO


ALTER PROCEDURE dbo.FechaConsultaMovil
AS

Declare @Fecha as date

Select top 1 @Fecha = Fecha_Registro				-- dateadd(day,10,Fecha_Registro)
from (
	Select distinct Fecha_Registro
	from (
		Select cast(substring(FECHA_REG,7,4) + '-' + substring(FECHA_REG,4,2) + '-' + substring(FECHA_REG,1,2) as date) as Fecha_Registro
		from NotificacionesMovil
	) A
) B
order by Fecha_Registro desc


Select FechaEtiqueta = right('0'+cast(datepart(day,@Fecha) as varchar(10)),2) + '/' + right('0'+cast(datepart(month,@Fecha) as varchar(10)),2)


GO

	-- Exec dbo.FechaConsultaMovil




/******************************/




ALTER PROCEDURE dbo.DataConsultaMovil
@COD_MOD char(7)
AS

	Select SE.COD_MOD, SE.ANEXO, SE.CEN_EDU, SE.NIV_MOD + ' - ' + NE.NombreNivel as NIVEL
	from ServicioEducativo SE
	join NivelEducativo NE on (SE.NIV_MOD = NE.CodNivel)
	where COD_MOD = @COD_MOD


	Select B.COD_MOD, B.CEN_EDU, B.NIV_MOD + ' - ' + NE.NombreNivel as NIV_MOD, B.FECHA_REG,				-- left(B.FECHA_REG,10) as FECHA_REG, 
			B.LATITUD, B.LONGITUD, B.[PRECISION], 
			OBSERVACIONES = case when (B.OBS_COORDS = 'Pendiente de Revisión' or B.OBS_FOTOS = 'Pendiente de Revisión') then 'Coordenadas recibidas. Pendiente de Revisión.' else B.OBS_COORDS + ' ' + B.OBS_FOTOS end
			, 
				Fotos = IsNull(
				Stuff((
					select ';' + Replace(isnull(PhotoName,''),',',';') from Movil_Fotos where substring(PhotoName,1,7) = B.COD_MOD
					for xml path('')
				),1,1,'') 
				,'')

	from (
			Select A.COD_MOD, MC.CEN_EDU, MC.NIV_MOD, A.FECHA_REG,
				A.LATITUD, A.LONGITUD, A.[PRECISION], 
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
		from (
			Select COD_MOD, FECHA_REG, LATITUD, LONGITUD, [PRECISION],
					Consist_CODGEO = Cat_CODGEO, 
					FTECOORDENADAS,
					Consist_LE = case when Dist_IE > 5000 then 2 else 1 end,
					Consist_IS = Consist_IS,
					Consist_Foto = REV_FOTOS
			from NotificacionesMovil
			where COD_MOD = @COD_MOD 
		) A
		left join ( Select COD_MOD, ANEXO, CEN_EDU, NIV_MOD from MarcoCenso2022 where ANEXO = 0 ) MC on (A.COD_MOD = MC.COD_MOD)
	) B
	left join NivelEducativo NE on (B.NIV_MOD = NE.CodNivel)
	
GO


		-- Exec DataConsultaMovil '1671916'

		-- 1671916  (8)
		-- 1671924  (1)
		-- 1718881  (0)



----------------------------------------------------------



Select distinct left(FECHA_REG,10) from NotificacionesMovil


Select * from NotificacionesMovil		-- 6
where 


Select * from ServicioEducativo
where COD_MOD not in (
Select COD_MOD from NotificacionesMovil		-- 6
)
and ESTADO = 1


Select * from ServicioEducativo
where COD_MOD = '1185908'

