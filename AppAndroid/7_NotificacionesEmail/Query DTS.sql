
--Select * from (


Select left(B.FECHA_REG,10) as FECHA_REG, B.COD_MOD, B.CEN_EDU, 
		NIVEL = B.NIV_MOD + ': ' + isnull(NE.NombreNivel,''), GESTION = B.GES_DEP + ': ' + isnull(GD.Descripcion,''), B.CODLOCAL, 
		OBSERVACIONES = case when (OBS_COORDS = 'Pendiente de Revisión' or OBS_FOTOS = 'Pendiente de Revisión') then 'Coordenadas recibidas. Pendiente de Revisión' else OBS_COORDS + ' ' + OBS_FOTOS end
from (
	Select A.FECHA_REG, A.COD_MOD, A.CODOOII, MC.CEN_EDU, MC.NIV_MOD, MC.GES_DEP, MC.CODLOCAL, 
		OBS_COORDS = 
			Case when (Consist_IS = -1 or Consist_Foto = -1)  then 'Pendiente de Revisión'
			else
				Case when (Consist_CODGEO = 1 and ((FTECOORDENADAS = 2 and Consist_LE = 1) or FTECOORDENADAS = 1) and Consist_IS <> 3 and Consist_Foto <> 5)
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
				when Consist_Foto = 1 then 'Foto bien tomada.' 
				else 'Sin foto o foto mal tomada o no se identifica la IE.'
				end
			end
	from (
		Select FECHA_REG, COD_MOD, CODLOCAL, CODOOII, LATITUD, LONGITUD, 
			Consist_CODGEO = Cat_CODGEO, FTECOORDENADAS,
			Consist_LE = case when Dist_IE > 5000 then 2 else 1 end,
			Consist_IS = Consist_IS,
			Consist_Foto = REV_FOTOS
			from NotificacionesMovil
			--where CODOOII = ?			
	) A
	left join ( 
		Select COD_MOD, ANEXO, CEN_EDU, NIV_MOD, GES_DEP, CODLOCAL 
		from MarcoCenso2022 
		where ANEXO = 0 
	) MC on (A.COD_MOD = MC.COD_MOD)
) B
left join NivelEducativo NE on (B.NIV_MOD = NE.CodNivel)
left join GestionDependencia GD on (B.GES_DEP = GD.CodGestion)
order by cast(substring(B.FECHA_REG,7,4) + substring(B.FECHA_REG,4,2) + substring(B.FECHA_REG,1,2) as datetime) 


--) XX where CEN_EDU is null