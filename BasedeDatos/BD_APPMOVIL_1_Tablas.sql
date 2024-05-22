USE [AplicativoMovil]
GO
/******   Table [dbo].[NivelEducativo]   ******/
CREATE TABLE [dbo].[NivelEducativo](
	[CodNivel] [char](2) NOT NULL,
	[NombreNivel] [varchar](100) NOT NULL,
	[LblCAED] [char](1) NULL,
 CONSTRAINT [PK_NivelEducativo] PRIMARY KEY CLUSTERED 
(
	[CodNivel] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/******   Table [dbo].[GestionDependencia]   ******/

CREATE TABLE [dbo].[GestionDependencia](
	[CodGestion] [char](2) NOT NULL,
	[Descripcion] [varchar](100) NOT NULL,
	[Observaciones] [varchar](255) NULL,
 CONSTRAINT [PK_GestionDependencia] PRIMARY KEY CLUSTERED 
(
	[CodGestion] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/******   Table [dbo].[ServicioEducativo]   ******/
CREATE TABLE [dbo].[ServicioEducativo](
	[IDServicio] [bigint] IDENTITY(1,1) NOT NULL,
	[COD_MOD] [char](7) NOT NULL,
	[ANEXO] [char](1) NOT NULL,
	[FORMAS] [char](1) NULL,
	[NIV_MOD] [char](2) NULL,
	[TIPOPROG] [char](2) NULL,
	[COD_CAR] [char](1) NULL,
	[COD_TUR] [char](2) NULL,
	[GES_DEP] [char](2) NULL,
	[CODOOII] [char](6) NULL,
	[REGISTRO] [char](1) NULL,
	[CODGEO] [char](6) NULL,
	[CEN_POB] [varchar](100) NULL,
	[CODCCPP] [char](6) NULL,
	[CEN_EDU] [varchar](100) NULL,
	[COD_AREA] [char](1) NULL,
	[AREA_SIG] [char](1) NULL,
	[DIR_CEN] [varchar](200) NULL,
	[CODLOCAL] [char](7) NULL,
	[FUENTE_LOCAL] [varchar](50) NULL,
	[NALT_LOCAL] [int] NULL,
	[CODCPSIG_OFICIAL] [char](6) NULL,
	[POINT_X] [numeric](38, 8) NULL,
	[POINT_Y] [numeric](38, 8) NULL,
	[FTECOORDENADA] [smallint] NULL,
	[ZOOM] [int] NULL,
	[TALUMNO] [int] NULL,
	[TDOCENTE] [int] NULL,
	[ANIO_CENSO] [int] NULL,
	[OBSERVACIONES] [varchar](255) NULL,
	[ESTADO_SINCRONIZACION] [tinyint] NOT NULL,
	[ESTADO] [char](1) NULL,
 CONSTRAINT [PK_ServicioEducativo] PRIMARY KEY CLUSTERED 
([IDServicio] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[ServicioEducativo]  WITH CHECK ADD  CONSTRAINT [FK_GestionDependencia] FOREIGN KEY([GES_DEP])
REFERENCES [dbo].[GestionDependencia] ([CodGestion])
GO

ALTER TABLE [dbo].[ServicioEducativo] CHECK CONSTRAINT [FK_GestionDependencia]
GO

ALTER TABLE [dbo].[ServicioEducativo]  WITH CHECK ADD  CONSTRAINT [FK_NivelEducativo] FOREIGN KEY([NIV_MOD])
REFERENCES [dbo].[NivelEducativo] ([CodNivel])
GO

ALTER TABLE [dbo].[ServicioEducativo] CHECK CONSTRAINT [FK_NivelEducativo]
GO

/******   Table [dbo].[CoordenadasMovil]   ******/
-- Tabla para la recepción de datos enviados por el app

Create table dbo.CoordenadasMovil (
	IdRow int not null identity(1,1),
	COD_MOD char(7) not null, 
	ANEXO char(1) not null,
	LATITUD	varchar(250) null,							-- numeric(38,8) null,	
	LONGITUD varchar(250) null,							-- numeric(38,8) null,	
	[PRECISION] varchar(250) null,						-- numeric(38,8) null,	
	ALTITUD varchar(250) null,							-- numeric(38,8) null,	
	OBSERVACIONES varchar(250) null,
	FOTO_PATH varchar(150) null,
	FECHA varchar(100) null,
	FECHA_REGISTRO datetime not null default(getdate()),
	Constraint PK_Coordenadas Primary Key Clustered (IdRow)
)