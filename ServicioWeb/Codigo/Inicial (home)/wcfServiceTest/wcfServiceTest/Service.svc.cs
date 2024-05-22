using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.Text;
using System.IO;
using System.Web.Script.Serialization;
using System.Data.SqlClient;
using System.Data;
using System.Collections.Specialized;
using System.Web;
using System.ServiceModel.Activation;

namespace wcfServiceTest
{
    // NOTA: puede usar el comando "Rename" del menú "Refactorizar" para cambiar el nombre de clase "Service" en el código, en svc y en el archivo de configuración a la vez.
    // NOTA: para iniciar el Cliente de prueba WCF para probar este servicio, seleccione Service.svc o Service.svc.cs en el Explorador de soluciones e inicie la depuración.    
    [AspNetCompatibilityRequirements(RequirementsMode = AspNetCompatibilityRequirementsMode.Allowed)]
    public class Service : IService
    {

        public CheckResponse CheckService(string ping)
        {
            if (ping == null)
                ping = "";

            return new CheckResponse(ping);
        }

        public string ValidateCode(string CodMod, string Anexo)
        {
            int intCheckCount = 0;
            string strCenEdu = "";

            if (CodMod is null || Anexo is null) {
                return strCenEdu;
            }

            string strDBConnection = System.Configuration.ConfigurationManager.ConnectionStrings["strDBConnectionPadron"].ConnectionString;

            SqlConnection connDB = new SqlConnection(strDBConnection);

            SqlCommand cmdSQLCheck = new SqlCommand
            {
                CommandType = CommandType.StoredProcedure,
                CommandText = "ValidaCodigo",
                Connection = connDB,
                CommandTimeout = 0
            };

            SqlParameter[] cmdSQLCheckparams = new SqlParameter[2];
            cmdSQLCheckparams[0] = new SqlParameter("@CodMod", CodMod);
            cmdSQLCheckparams[1] = new SqlParameter("@Anexo", Anexo);
            cmdSQLCheck.Parameters.AddRange(cmdSQLCheckparams);

            SqlDataAdapter daDBCommandCheck = new SqlDataAdapter(cmdSQLCheck);

            DataSet dsDataCheck = new System.Data.DataSet();

            daDBCommandCheck.Fill(dsDataCheck);

            intCheckCount = dsDataCheck.Tables[0].Rows.Count;

            if (intCheckCount > 0) {
                strCenEdu = dsDataCheck.Tables[0].Rows[0][2].ToString();
            }

            return toJSONFormat2(CodMod + Anexo, intCheckCount, strCenEdu);
            /*
            ResponseData responseData = new ResponseData(CodMod + Anexo, intCheckCount, strCenEdu);
            var json = new JavaScriptSerializer().Serialize(responseData);
            return new CheckResponse(json);
            */
            //return name;
        }

        public string RegisterData(Stream data)
        {

            string StringData;
            string CodMod, Anexo, Latitud, Longitud, Precision, Altitud, Fecha, Foto;
            string strResult = "", strCenEdu = "";

            int intInsertResult = 0;
            int intCheckCount = 0;

            StringData = new StreamReader(data).ReadToEnd();

            if (String.IsNullOrWhiteSpace(StringData)) {
                return "0";
            }

            NameValueCollection ArrayData = HttpUtility.ParseQueryString(StringData);

            CodMod = ArrayData["CodMod"];
            Anexo = ArrayData["Anexo"];
            Latitud = ArrayData["Latitud"];
            Longitud = ArrayData["Longitud"];
            Precision = ArrayData["Precision"];
            Altitud = ArrayData["Altitud"];
            Fecha = ArrayData["Fecha"];
            Foto = ArrayData["Foto"];

            if (String.IsNullOrWhiteSpace(CodMod) || String.IsNullOrWhiteSpace(Anexo)) {
                return "1";
            }

            string strDBConnection = System.Configuration.ConfigurationManager.ConnectionStrings["strDBConnectionPadron"].ConnectionString;

            SqlConnection connDB = new SqlConnection(strDBConnection);


            // Validacion
            SqlCommand cmdSQLCheck = new SqlCommand
            {
                CommandType = CommandType.StoredProcedure,
                CommandText = "ValidaCodigo",
                Connection = connDB,
                CommandTimeout = 0
            };

            SqlParameter[] cmdSQLCheckparams = new SqlParameter[2];
            cmdSQLCheckparams[0] = new SqlParameter("@CodMod", CodMod);
            cmdSQLCheckparams[1] = new SqlParameter("@Anexo", Anexo);
            cmdSQLCheck.Parameters.AddRange(cmdSQLCheckparams);

            SqlDataAdapter daDBCommandCheck = new SqlDataAdapter(cmdSQLCheck);

            DataSet dsDataCheck = new System.Data.DataSet();

            daDBCommandCheck.Fill(dsDataCheck);

            intCheckCount = dsDataCheck.Tables[0].Rows.Count;

            if (intCheckCount > 0)
            {
                strCenEdu = dsDataCheck.Tables[0].Rows[0][2].ToString();
            }
            // 


            string FotoPath = System.Configuration.ConfigurationManager.AppSettings["ImagesFolder"].ToString();  //HttpContext.Current.Server.MapPath("/Uploads");

            FotoPath = FotoPath + CodMod + ".jpg";

            /*
            try
            {
            */
                // Registro
                SqlCommand cmdSQLInsert = new SqlCommand
                {
                    CommandType = CommandType.StoredProcedure,
                    CommandText = "RegistraDatosMovil",
                    Connection = connDB,
                    CommandTimeout = 0
                };

                SqlParameter[] cmdSQLparams = new SqlParameter[8];
                cmdSQLparams[0] = new SqlParameter("@CodMod", CodMod);
                cmdSQLparams[1] = new SqlParameter("@Anexo", Anexo);
                cmdSQLparams[2] = new SqlParameter("@Latitud", Latitud);
                cmdSQLparams[3] = new SqlParameter("@Longitud", Longitud);
                cmdSQLparams[4] = new SqlParameter("@Precision", Precision);
                cmdSQLparams[5] = new SqlParameter("@Altitud", Altitud);
                cmdSQLparams[6] = new SqlParameter("@FotoPath", FotoPath);
                cmdSQLparams[7] = new SqlParameter("@Fecha", Fecha);

                cmdSQLInsert.Parameters.AddRange(cmdSQLparams);
                connDB.Open();
                cmdSQLInsert.ExecuteNonQuery();
                connDB.Close();

                if (!String.IsNullOrWhiteSpace(Foto))
                {
                    byte[] FotoImageFormat = Convert.FromBase64String(Foto);
                    System.IO.File.WriteAllBytes(FotoPath, FotoImageFormat);
                }

                intInsertResult = 1;
            /*
            }
            catch {
                intInsertResult = 0;
            }
            */
            return toJSONFormat(CodMod+Anexo, intInsertResult, intCheckCount, strCenEdu);
        }

        private string toJSONFormat2(string Id, int Check, string ItemName)
        {
            string strJSONResult;

            if (Check >= 1)
            {
                strJSONResult = "{" +
                        "\"Result\":[" +
                            "{" +
                            "\"Status\":\"" + Check.ToString() + "\", " +
                            "\"COD_MOD\":\"" + Id.Substring(0, 7) + "\", " +
                            "\"ANEXO\":\"" + Id.Substring(7, 1) + "\", " +
                            "\"CEN_EDU\":\"" + ItemName + "\"" +
                            "}" +
                        "]" +
                    "}";
            }
            else
            {
                strJSONResult = "{" +
                    "\"Result\":[" +
                        "]" +
                    "}";

            }
            return strJSONResult;
        }

        private string toJSONFormat(string Id, int Result, int Check, string ItemName) {
            string strJSONResult;

            if (Result == 1)
            {
                strJSONResult = "{" +
                        "\"Result\":" +
                            "{" +
                            "\"Status\":\"" + Check.ToString() + "\", " +
                            "\"COD_MOD\":\"" + Id.Substring(0, 7) + "\", " +
                            "\"ANEXO\":\"" + Id.Substring(7, 1) + "\", " +
                            "\"CEN_EDU\":\"" + ItemName + "\"" +
                            "}" +
                    "}";
            }
            else {
                strJSONResult = "";
                /*
                strJSONResult = "{" +
                    "\"Result\":" +
                        "{" +
                        "}" +
                    "}";
                */
            }
            return strJSONResult;
        }


        public string RegisterDataInitial(Stream data)
        {

            string InsertResult = "0";

            string StringData = new StreamReader(data).ReadToEnd();
            NameValueCollection ArrayData = HttpUtility.ParseQueryString(StringData);

            string CodMod = ArrayData["CodMod"];
            string Anexo = ArrayData["Anexo"];
            string Latitud = ArrayData["Latitud"];
            string Longitud = ArrayData["Longitud"];
            string Precision = ArrayData["Precision"];
            string Foto = ArrayData["Foto"];

            string FotoPath = System.Configuration.ConfigurationManager.AppSettings["ImagesFolder"].ToString();  //HttpContext.Current.Server.MapPath("/Uploads");

            FotoPath = FotoPath + CodMod + ".jpg";

            string strDBConnection = System.Configuration.ConfigurationManager.ConnectionStrings["strDBConnectionPadron"].ConnectionString;

            SqlConnection connDB = new SqlConnection(strDBConnection);

            SqlCommand cmdSQL = new SqlCommand
            {
                CommandType = CommandType.StoredProcedure,
                CommandText = "RegistraDatosMovil",
                Connection = connDB,
                CommandTimeout = 0
            };

            SqlParameter[] cmdSQLparams = new SqlParameter[6];
            cmdSQLparams[0] = new SqlParameter("@CodMod", CodMod);
            cmdSQLparams[1] = new SqlParameter("@Anexo", Anexo);
            cmdSQLparams[2] = new SqlParameter("@Latitud", Latitud);
            cmdSQLparams[3] = new SqlParameter("@Longitud", Longitud);
            cmdSQLparams[4] = new SqlParameter("@Precision", Precision);
            cmdSQLparams[5] = new SqlParameter("@FotoPath", FotoPath);

            cmdSQL.Parameters.AddRange(cmdSQLparams);
            connDB.Open();
            cmdSQL.ExecuteNonQuery();
            connDB.Close();

            byte[] FotoImageFormat = Convert.FromBase64String(Foto);
            System.IO.File.WriteAllBytes(FotoPath, FotoImageFormat);

            InsertResult = "1";

            return InsertResult;
        }


        public class ResponseData
        {
            public string Status { get; set; }
            public string COD_MOD { get; set; }
            public string ANEXO { get; set; }
            public string CEN_EDU { get; set; }

            public ResponseData(string Id, int Check, string ItemName)
            {
                this.Status = Check.ToString();
                this.COD_MOD = Id.Substring(0, 7);
                this.ANEXO = Id.Substring(7, 1);
                this.CEN_EDU = ItemName;
            }

        }

    }

}
