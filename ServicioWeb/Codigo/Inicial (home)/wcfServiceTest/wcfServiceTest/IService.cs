using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.Text;
using System.ServiceModel.Web;
using System.IO;

namespace wcfServiceTest
{
    // NOTA: puede usar el comando "Rename" del menú "Refactorizar" para cambiar el nombre de interfaz "IService" en el código y en el archivo de configuración a la vez.
    [ServiceContract]
    public interface IService
    {

        [OperationContract]
        [WebGet(RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json)]
        CheckResponse CheckService(string ping);


        [OperationContract]
        [WebGet(RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json)]
        string ValidateCode(string CodMod, string Anexo);


        [OperationContract]
        [WebInvoke(Method = "POST", BodyStyle = WebMessageBodyStyle.WrappedRequest)]
        string RegisterData(Stream data);


        [OperationContract]
        [WebInvoke(Method="POST", ResponseFormat =WebMessageFormat.Json, RequestFormat =WebMessageFormat.Json, BodyStyle =WebMessageBodyStyle.WrappedRequest)]
        string RegisterDataInitial(Stream data);

    }

    [DataContract]
    public class CheckResponse {

        [DataMember]
        public string Result { get; set; }

        public CheckResponse(string ping) { 
            this.Result = ping;
        }

    }

}
