Vue.config.devtools = true

var vue_det = new Vue({
    el: '#MainApp',
    data: {
        message_2080: false,
        message_2083: false,
        messages_download_params: null,
        form: {
            LocoID: null,
            StartDate: null,
            EndDate: null,
            StartTime: null,
            EndTime: null,
            SCACMark: null,
            RequestType: null,
            timeZone: 0,
            dst: false,
            messageType: 0
        },
        data_2080: {},
        data_2083: {},
        lstFullTree: [{
            value: null,
            text: 'Please select an option'
        }],
        lstTimeZone:
        		[
            	{ text: 'Eastern' , value: 5},
            	{ text: 'Central' , value: 6},
            	{ text: 'Mountain' , value: 7},
            	{ text: 'Pacific' , value: 8},
            	{ text: 'UTC' , value: 0}
            	],
        lstMessageType:
        		[
            	{ text: 'All' , value: 0},
            	{ text: '2080' , value: 2080},
            	{ text: '2083' , value: 2083}
            	],
        tab_data_Array: [],
        show: true,
        timeUTC: '',
        userName: '',
        formTZ: '',
        cell_class: 'table-danger',
        isActive: false,
        active_tab: '',
        plugins: [],
        S3BaseUrl: '',
        mode: 'main',
        message_loading: false,
        downloading: false,
        locomotives_columns: [   {label: "Locomotive", field: "Locomotive", filterable:true},
                                 {label: "ATT Modem", field: "ATTModem", filterable:true},
                                 {label: "ATT Modem IsOnline", field: "ATTModemIsOnline", hidden:true},
                                 {label: "VZW Modem", field: "VZWModem", filterable:true},
                                 {label: "VZW Modem IsOnline", field: "VZWModemIsOnline", hidden:true},
                                 {label: "WiFi", field: "WiFi", filterable:true},
                                 {label: "WiFi IsOnline", field: "WiFiIsOnline", hidden:true},
                                 {label: "220", field: "Radio", filterable:true},
                                 {label: "Radio IsOnline", field: "RadioIsOnline", hidden:true}],

        locomotives_updates_columns: [
                                         {label: "Timestamp", field: "Timestamp", filterable:true},
                                         {label: "ATT Modem", field: "ATTModem", filterable:true},
                                         {label: "ATT Modem IsOnline", field: "ATTModemIsOnline", hidden:true},
                                         {label: "VZW Modem", field: "VZWModem", filterable:true},
                                         {label: "VZW Modem IsOnline", field: "VZWModemIsOnline", hidden:true},
                                         {label: "WiFi", field: "WiFi", filterable:true},
                                         {label: "WiFi IsOnline", field: "WiFiIsOnline", hidden:true},
                                         {label: "220", field: "Radio", filterable:true},
                                         {label: "Radio IsOnline", field: "RadioIsOnline", hidden:true},
                                         ],
        reports_columns: [{label: "Type", field: "type", filterable:true},
                          {label: "Period", field: "period", filterable:true},
                          {label: "Month/Quoter", field: "month", filterable:true},
                          {label: "Year", field: "year", filterable:true},
                          {label: "URL", field: "url", filterable:true},
                         ],
        locomotives_rows: [],
        locomotives_timestamp: "",
        messages_rows: [],
        reports_rows: [],
        messages_additional_rows: [],
        messages_columns: [
            {label: "Locomotive ID", field: "address", filterable:true},
            {label: "Message Type", field: "messageType", filterable:true},
            {label: "Timestamp", field: "timestamp", filterable:true},
        ],
    },
    created: function () {
        this.getUserName()
        this.fetchData()
        this.initDateTime()
        this.timeUTC = this.getUTCTime()
        this.timer = setInterval(this.getUTCTime, 5000)
    },
    methods: {
        openMessageDetails(messageIndex) {
            var details = this.messages_additional_rows[messageIndex]
            var messageType = details.idType

            if (messageType === 2080) {
                this.data_2080 = JSON.parse(JSON.stringify(details))
            }
            if (messageType === 2083) {
                this.data_2083 = JSON.parse(JSON.stringify(details))
            }

            this.message_2083 = messageType === 2083
            this.message_2080 = messageType === 2080

            this.showModal = true;
        },
        setRequestType(value) {
            this.form.RequestType = value
            if (this.form.RequestType == "get-status")
            {
               LocoIdElement = document.getElementById("inpLocoID");
               LocoIdElement.setCustomValidity(this.validateLocoId()? "" : "LocoId should be set for this request");
            }
        },

        validateLocoId() {
           return this.form.LocoID != null
        },

        GetLocomotivesReportBack() {
           this.mode = 'main';
        },

        getLocoID(source) {
            var dot = source.indexOf(".")
            var locoID = source.substring(dot + 1)
            dot = locoID.indexOf(".")
            locoID = locoID.substring(dot + 1)
            var colon = locoID.indexOf(":")
            locoID = locoID.substring(0, colon)
            return locoID
        },

        downloadMessages(messageType) {
            this.downloading = true;
            var xhr = new XMLHttpRequest()
            var self = this
            xhr.open('POST', 'locomotive-messages.csv', true)
            xhr.responseType = 'blob';
            xhr.onload = function () {
                if (this.status === 200) {
                    var blob = this.response;
                    var filename = "report_" + messageType + "_" +
                        self.form.StartDate + "_" + self.form.StartTime + "_" +
                        self.form.EndDate + "_" + self.form.EndTime +
                        ".csv";

                    if (typeof window.navigator.msSaveBlob !== 'undefined') {
                        // IE workaround for "HTML7007: One or more blob URLs were revoked by closing the blob for which they were created. These URLs will no longer resolve as the data backing the URL has been freed."
                        window.navigator.msSaveBlob(blob, filename);
                    } else {
                        var URL = window.URL || window.webkitURL;
                        var downloadUrl = URL.createObjectURL(blob);

                        if (filename) {
                            // use HTML5 a[download] attribute to specify filename
                            var a = document.createElement("a");
                            // safari doesn't support this yet
                            if (typeof a.download === 'undefined') {
                                window.location.href = downloadUrl;
                            } else {
                                a.href = downloadUrl;
                                a.download = filename;
                                document.body.appendChild(a);
                                a.click();
                            }
                        } else {
                            window.location.href = downloadUrl;
                        }

                        setTimeout(function () { URL.revokeObjectURL(downloadUrl); }, 100); // cleanup
                    }
                    self.downloading = false;
                }
            };
            var formData = JSON.parse(this.messages_download_params)
            formData.messageType = messageType
            xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
            xhr.send(JSON.stringify(formData))
        },

        GetLocomotiveMessages() {
            this.mode = 'messages';
            this.message_loading = true;

            this.messages_download_params = JSON.stringify(this.form, (key, value)=> {
                    if ((value === null) && (key === 'LocoID')) return undefined
                    return value
            })

            var xhr = new XMLHttpRequest()
            var self = this
            xhr.open('POST', 'locomotive-messages')
            xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')

            self.messages_rows = [];
            self.messages_additional_rows = [];

            xhr.onload = async function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        var data = JSON.parse(xhr.responseText);
                        for (var i = 0; i < data.messages.length; i++) {
                            self.messages_rows.push ({
                                "id": i,
                                "address": self.getLocoID(data.messages[i].srcAddress),
                                "messageType": data.messages[i].idType.toString(),
                                "timestamp": formatToUTC(data.messages[i].time * 1000)
                            });
                            data.messages[i].stateTimeUTC = formatToUTC(data.messages[i].stateTime)
                            data.messages[i].warningTimeUTC = formatToUTC(data.messages[i].warningTime)
                            data.messages[i].enforcementTimeUTC =  formatToUTC(data.messages[i].enforcementTime)
                            data.messages[i].emergencyEnforcementTimeUTC =  formatToUTC(data.messages[i].emergencyEnforcementTime)
                            data.messages[i].currentTimeUTC = formatToUTC(data.messages[i].currentTime)
                            self.messages_additional_rows.push(data.messages[i]);
                        }
                        self.message_loading = false;
                    } else {
                        var errorMessage = JSON.parse(xhr.responseText).message
                        self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                        console.error('error - ' + errorMessage)
                    }
                }
            }
            xhr.send(this.messages_download_params)
        },

        GetLocomotivesReport() {
           this.mode = 'locomotives';

           var xhr = new XMLHttpRequest()
           var self = this
           xhr.open('GET', 'locomotives.json')
           xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')

           self.locomotives_rows = [];

           xhr.onload = async function () {
             if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                   var locomotivesData = JSON.parse(xhr.responseText);
                   self.locomotives_timestamp = locomotivesData.StartTestTime
                   for (var i = 0; i < locomotivesData.Locomotives.length; i++)
                      self.locomotives_rows.push (
                           {id:i, Locomotive: locomotivesData.Locomotives[i].SCAC + "-" +locomotivesData.Locomotives[i].LocoID,
                            ATTModem: locomotivesData.Locomotives[i].ATTModem.Address,
                            ATTModemIsOnline: locomotivesData.Locomotives[i].ATTModem.IsOnline,
                            VZWModem: locomotivesData.Locomotives[i].VZWModem.Address,
                            VZWModemIsOnline: locomotivesData.Locomotives[i].VZWModem.IsOnline,
                            WiFi: locomotivesData.Locomotives[i].WiFi.Address,
                            WiFiIsOnline: locomotivesData.Locomotives[i].WiFi.IsOnline,
                            Radio: locomotivesData.Locomotives[i].Radio.Address,
                            RadioIsOnline: locomotivesData.Locomotives[i].Radio.IsOnline,});

                } else {
                    var errorMessage = JSON.parse(xhr.responseText).message
                    self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                    console.error('error - ' + errorMessage)
                }
             }
           }
           xhr.send()
        },
        GetQuoterMonthReports() {
           this.mode = 'reports';
           var xhr = new XMLHttpRequest()
           var self = this
           xhr.open('GET', 'reports.json')
           xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')

           self.reports_rows = [];

           xhr.onload = async function () {
           if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    var reportsData = JSON.parse(xhr.responseText);
                    for (var i = 0; i < reportsData.length; i++)
                         self.reports_rows.push (
                             {type: reportsData[i].type,
                              period: reportsData[i].period,
                              month: self.getMonth(reportsData[i].term, reportsData[i].period),
                              year: reportsData[i].year,
                              url: reportsData[i].url});

                           } else {
                               var errorMessage = JSON.parse(xhr.responseText).message
                               self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                               console.error('error - ' + errorMessage)
                           }
                        }
                      }
                 xhr.send()
        },

        onSubmit(evt) {
            evt.preventDefault()
            var xhr = new XMLHttpRequest()
            var self = this
            var requestSCACMark = self.form.SCACMark
            var requestLocoID = self.form.LocoID

            xhr.open('POST', 'data-request')
            xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')

            xhr.onload = async function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        var messageId = xhr.responseText
                        var index = self.tab_data_Array.length
                        var timer = setInterval(self.checkResponse.bind(null, messageId), 1000)

                        if (self.form.RequestType == "get-status") {
                            var title = "(" + index + ") " + requestSCACMark + " " + requestLocoID + " status"
                            var tabItem = createStatusTab(title, index, self.form, messageId, timer)
                            // send additional request to get the list of the last statuses
                            self.sendLocomotiveUpdateRequest(tabItem, requestSCACMark, requestLocoID)
                        } else if (self.form.RequestType == "get-logs") {
                            var title = "(" + index + ") " + requestSCACMark + " " + requestLocoID + " logs"
                            var tabItem = createLogsTab(title, index, self.form, messageId, timer)
                        } else if (self.form.RequestType == "get-backoffice") {
                            var title = "(" + index + ") " + requestSCACMark + " office"
                            var tabItem = createOfficeTab(title, index, self.form, messageId, timer)
                            // Send additional request to get Federation status
                            self.sendFederationRequest(requestSCACMark, messageId)
                        } else {
                            console.error("Unknown type of request!")
                            return
                        }
                        self.tab_data_Array.push(tabItem)

                        self.$nextTick(function () {
                            self.active_tab = title
                        });
                      }
                    } else {
                        var errorMessage = JSON.parse(xhr.responseText).message
                        self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                        console.error('error - ' + errorMessage)
                    }
            }
            xhr.send(JSON.stringify(this.form, (key, value)=> {
              if ((value === null) && (key === 'LocoID')) return undefined
                  return value
            }))
        },
        sendFederationRequest(SCACMark, officeMessageId) {
            var xhr = new XMLHttpRequest()
            xhr.open('POST', 'data-request')
            xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')

            var self = this

            xhr.onload = async function () {
                if (xhr.readyState !== 4)
                    return
                if (xhr.status === 200) {
                    var messageId = xhr.responseText
                    var timer = setInterval(self.checkResponse.bind(null, messageId), 1000)
                    self.addFederationInfo(messageId, officeMessageId, timer)
                } else {
                    var errorMessage = JSON.parse(xhr.responseText).message
                    self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                    console.error('error - ' + errorMessage)
                }
            }

            var param = {
                RequestType: "get-federation",
                SCACMark: SCACMark,
            }

            xhr.send(JSON.stringify(param))
        },
        addFederationInfo(federationMessageId, officeMessageId, timer) {
            for (var i=0; i < this.tab_data_Array.length; i++) {
                if (this.tab_data_Array[i].messageIds.includes(officeMessageId)) {
                    this.tab_data_Array[i].messageIds.push(federationMessageId)
                    this.tab_data_Array[i].federationTimer = timer
                    break
                }
            }
        },
        sendLocomotiveUpdateRequest(tabItem, SCACMark, locoId) {
             var xhr = new XMLHttpRequest()
             const requestUrl = 'locomotive-update.json/'+SCACMark+'/'+locoId;
             xhr.open('GET', requestUrl)
             xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')

             var self = this
             tabItem.locomotives_rows = [];

             xhr.onload = async function () {
                if (xhr.readyState !== 4)
                   return
                if (xhr.status === 200) {
                     var locomotivesData = JSON.parse(xhr.responseText);
                     for (var i = 0; i < locomotivesData.status_updates.length; i++)
                        tabItem.locomotives_rows.push (
                           {id:i, Locomotive: null,
                               ATTModem: locomotivesData.status_updates[i].ATTModem.Address,
                               ATTModemIsOnline: locomotivesData.status_updates[i].ATTModem.IsOnline,
                               VZWModem: locomotivesData.status_updates[i].VZWModem.Address,
                               VZWModemIsOnline: locomotivesData.status_updates[i].VZWModem.IsOnline,
                               WiFi: locomotivesData.status_updates[i].WiFi.Address,
                               WiFiIsOnline: locomotivesData.status_updates[i].WiFi.IsOnline,
                               Radio: locomotivesData.status_updates[i].Radio.Address,
                               RadioIsOnline: locomotivesData.status_updates[i].Radio.IsOnline,
                               Timestamp: formatter.format(new Date(locomotivesData.status_updates[i].Timestamp))});
                }
              }
             xhr.send()
        },

        onReset(evt) {
            evt.preventDefault()
            this.form.LocoID = null
            this.form.SCACMark = null
            this.locomotives_rows=[]
            this.initDateTime()
            // Trick to reset/clear native browser form validation state
            this.show = false
            // stop all timers
            for (var i=0; i < this.tab_data_Array.length; i++) {
               clearInterval(this.tab_data_Array[i].timer)
            }
            this.tab_data_Array = []
            this.$nextTick(() => {
                this.show = true
            })
        },
        initDateTime: function () {
            this.form.StartDate = this.formatDate()
            this.form.EndDate = this.form.StartDate
            this.form.StartTime = '00:01'
            this.form.EndTime = '23:59'
        },
        fetchData: function () {
            var xhr = new XMLHttpRequest()
            var self = this
            var dataSCAC

            xhr.open('GET', 'railroads.json')
            xhr.onload = function () {
                conf = JSON.parse(xhr.responseText)
                self.lstFullTree = self.lstFullTree.concat(conf.SCAC)
                if (conf.Plugins) {
                    console.log("Plugins: " + conf.Plugins)
                    conf.Plugins.forEach(function (item) {
                      self.plugins.push(item)
                    })
                }
                if (conf.S3BaseURL) {
                    self.S3BaseURL = conf.S3BaseURL
                }
            }
            xhr.send()
        },
        getDstState: function () {
        	var today = new Date();
            var jan = new Date(today.getFullYear(), 0, 1);
            var jul = new Date(today.getFullYear(), 6, 1);
            var hiTZ = Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset());
            if (today.getTimezoneOffset() < hiTZ){
            	console.log("DST detected");
            }
        },
        getUserName: function () {
            var xhr = new XMLHttpRequest()
            var self = this
            var dataUser

            xhr.open('GET', 'user.json')
            xhr.onload = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        dataUser = JSON.parse(xhr.responseText)
                        self.userName = dataUser.userName
                    } else {
                        self.userName = ''
                        console.error('error - ' + xhr.statusText);
                    }
                }
            }
            xhr.send()
        },

        formatDate: function () {
            var d = new Date(),
                month = '' + (d.getMonth() + 1),
                day = '' + d.getDate(),
                year = d.getFullYear();

            if (month.length < 2)
                month = '0' + month;
            if (day.length < 2)
                day = '0' + day;

            return [year, month, day].join('-');
        },

        getUTCTime: function () {
            var d = new Date();
            var hr = d.getUTCHours();
            var min = d.getUTCMinutes();
            var res;

            if (hr < 10) hr = '0' + hr;
            if (min < 10) min = '0' + min;

            res = hr + ":" + min
            this.timeUTC = res
            return res
        },
        getMonth: function (num, period) {
         if (period === "month") {
           const monthNames = ["January", "February", "March", "April", "May", "June",
             "July", "August", "September", "October", "November", "December"
           ];
           return monthNames[num];
          }

          return num;
        },

        checkResponse: function (ourMessageId) {
            var xhr = new XMLHttpRequest()
            var self = this
            xhr.open('GET', 'status-update/' + ourMessageId)
            xhr.onload = function () {
                if (xhr.readyState !== 4)
                    return
                if (xhr.status === 200) {
                    console.log("xhr.responseText=", xhr.responseText)
                    var statusUpdate = JSON.parse(xhr.responseText)
                    for (var i=0; i < self.tab_data_Array.length; i++) {
                        if (self.tab_data_Array[i].messageIds.includes(ourMessageId)) {
                            updateTab(self.tab_data_Array[i], statusUpdate)
                            break
                        }
                    }
                } else if (xhr.status === 204) {
                    // no update, try again later
                }
                else {
                    console.error('error - ' + xhr.statusText);
                }
            }
            xhr.send()
        },
    }
});

function createStatusTab(title, index, form, messageId, timer) {
    return {
        id:index,
        title: title,
        LocoID: form.SCACMark + " " + form.LocoID,
        status_data: fillGetStatusData(),
        messageIds: [messageId],
        messages:["Loading locomotive system status"],
        timer: timer,
        sLogRequestStatus: "Request submitted successfully",
        showFooter: false, isLogs: false, isStatus: true, isOffice: false, showLinks: false,
        locomotives_rows: []
    }
}

function createLogsTab(title, index, form, messageId, timer) {
    return {
        id: index,
        title: title,
        LocoID: form.LocoID,
        messageIds: [messageId],
        messageId: messageId,
        Status:"Loading locomotive logs...",
        timer: timer,
        showFooter: true, isLogs: true, isStatus: false, isOffice: false, showLinks: false,
        sFilesCount: "Counting...",
        sTotalBytes: "Checking log file...",
        sLogRequestStatus: "Request submitted successfully"
    }
}

function createOfficeTab(title, index, form, messageId, timer) {
    return {
        id: index,
        title: title,
        messageIds: [messageId],
        Status:"Loading backoffice status...",
        office_dataArray: fillOfficeData(),
        messages:["Loading backoffice system status"],
        federations: [],
        timer: timer,
        showFooter: false, isLogs: false, isStatus: false, isOffice: true, showLinks: false,
        sLogRequestStatus: "Request submitted successfully"
    }
}

function updateTab(tabItem, statusUpdate) {
    tabItem.showFooter = false

    // Locomotive status tab has an array of status messages, which is growing with time
    if (statusUpdate.statusText && tabItem.messages) {
        tabItem.messages.push(statusUpdate.statusText)
    }

    if (statusUpdate.TestTime)
        tabItem.status_data.TestTime = statusUpdate.TestTime

    // ip information
    if (statusUpdate.VerizonModem)
        tabItem.status_data.IpInformation.VerizonModem = statusUpdate.VerizonModem
    if (statusUpdate.ATTModem)
        tabItem.status_data.IpInformation.ATTModem = statusUpdate.ATTModem

    // Ip reachability
    if (statusUpdate.ATTModemStatus) {
        tabItem.status_data.IpReachability.ATTModemStatus = statusUpdate.ATTModemStatus
        tabItem.status_data.IpReachability.showConnectedATTConnected =
            !statusUpdate.ATTModemStatus.toLowerCase().includes("unreach")
    }
    if (statusUpdate.VerizonModemStatus) {
        tabItem.status_data.IpReachability.VerizonModemStatus = statusUpdate.VerizonModemStatus
        tabItem.status_data.IpReachability.showConnectedVerizonConnected =
            statusUpdate.VerizonModemStatus.toLowerCase().includes("online")
    }

    // Wifi information
    if (statusUpdate.WiFiClientStatus)
    {
        tabItem.status_data.WifiInformation.ClientStatus = statusUpdate.WiFiClientStatus
        tabItem.status_data.WifiInformation.showConnected =
            !statusUpdate.WiFiClientStatus.toLowerCase().includes("not connected")
    }
    if (statusUpdate.ClientId)
        tabItem.status_data.WifiInformation.ClientId = statusUpdate.ClientId
    if (statusUpdate.AccessPoint)
        tabItem.status_data.WifiInformation.AccessPoint = statusUpdate.AccessPoint

    // gateway information
    if (statusUpdate.ETMSClient) {
        tabItem.status_data.GatewayInformation.ETMSClient = statusUpdate.ETMSClient
        tabItem.status_data.GatewayInformation.ETMSConnected = statusUpdate.ETMSClient.toLowerCase() === "connected"
    }
    if (statusUpdate.MDMClient) {
        tabItem.status_data.GatewayInformation.MDMClient = statusUpdate.MDMClient
        tabItem.status_data.GatewayInformation.MDMConnected = statusUpdate.MDMClient.toLowerCase() === "connected"
    }

    // route information
    if (statusUpdate.ATTRoute) {
        tabItem.status_data.RouteInformation.ATTRoute = statusUpdate.ATTRoute
        tabItem.status_data.RouteInformation.isATTStable = statusUpdate.ATTRoute.toLowerCase() === "connected"
    }
    if (statusUpdate.ATTRouteTimestamp) {
        tabItem.status_data.RouteInformation.ATTRouteTimestamp = statusUpdate.ATTRouteTimestamp
    }

    if (statusUpdate.VerizonRoute){
        tabItem.status_data.RouteInformation.VerizonRoute = statusUpdate.VerizonRoute
        tabItem.status_data.RouteInformation.isVerizonStable = statusUpdate.VerizonRoute.toLowerCase() === "connected"
    }
    if (statusUpdate.VerizonRouteTimestamp) {
        tabItem.status_data.RouteInformation.VerizonRouteTimestamp = statusUpdate.VerizonRouteTimestamp
    }

    if (statusUpdate.SprintRoute){
        tabItem.status_data.RouteInformation.SprintRoute = statusUpdate.SprintRoute
        tabItem.status_data.RouteInformation.showSprintStable =
            !statusUpdate.SprintRoute.toLowerCase().includes("not connected")
    }
    if (statusUpdate.SprintRouteTimestamp) {
        tabItem.status_data.RouteInformation.SprintRouteTimestamp = statusUpdate.SprintRouteTimestamp
    }

    if (statusUpdate.WifiRoute) {
        tabItem.status_data.RouteInformation.WifiRoute = statusUpdate.WifiRoute
        tabItem.status_data.RouteInformation.showRouteConnected =
            !statusUpdate.WifiRoute.toLowerCase().includes("not connected")
    }
    if (statusUpdate.WiFiRouteTimestamp) {
        tabItem.status_data.RouteInformation.WiFiRouteTimestamp = statusUpdate.WiFiRouteTimestamp
    }

    if (statusUpdate.MHzRadio) {
        tabItem.status_data.RouteInformation.MHzRadio = statusUpdate.MHzRadio
        tabItem.status_data.RouteInformation.showRadioConnected =
            !statusUpdate.MHzRadio.toLowerCase().includes("not connected")
    }
    if (statusUpdate.RadioRouteTimestamp) {
        tabItem.status_data.RouteInformation.RadioRouteTimestamp = statusUpdate.RadioRouteTimestamp
    }

    // gateway information
    if (statusUpdate.RadioId)
        tabItem.status_data.RadioInformation.RadioId = statusUpdate.RadioId
    if (statusUpdate.EMPAddress)
        tabItem.status_data.RadioInformation.EMPAddress = statusUpdate.EMPAddress

    if (statusUpdate.CellStatus) {
        tabItem.status_data.CellStatus.status = statusUpdate.CellStatus
        tabItem.status_data.CellStatus.showGood = statusUpdate.CellStatus.toLowerCase().includes("good")
    }

    // logs information
    if (statusUpdate.filesCount)
        tabItem.sFilesCount = statusUpdate.filesCount
    if (statusUpdate.totalBytes)
        tabItem.sTotalBytes = statusUpdate.totalBytes
    if (statusUpdate.statusText)
    {
        if ((statusUpdate.Status != "2000"))
            tabItem.Status = statusUpdate.statusText
    }
    if (statusUpdate.end === "1") {
        clearInterval(tabItem.timer)
    if (tabItem.isLogs)
        tabItem.showLinks = true
    }

    // backoffice information
    if (statusUpdate.Status === "2000") {
        console.log("ServerType: " + statusUpdate.ServerType)
        console.log("ServerCount: " + statusUpdate.ServerCount)
        console.log("ServerPostfix: " + statusUpdate.ServerPostfix)
        console.log("ServerStatus: " + statusUpdate.ServerStatus)

        if (tabItem.office_dataArray.length > 0) {
           var array_len = tabItem.office_dataArray.length
           tabItem.office_dataArray[array_len-1].servers.push( {descr: statusUpdate.ServerType + " " + statusUpdate.ServerCount,
               value: statusUpdate.ServerStatus, errorMsg: ''})
        }

    }

    if (statusUpdate.Status === "2003") {
        console.log("Header: " + statusUpdate.statusText)

        tabItem.Status = "Loading " + statusUpdate.statusText

        office_data = {
           header: statusUpdate.statusText,
           servers: []
        }
        tabItem.office_dataArray.push(office_data)
    }

    if (statusUpdate.Status === "2001")
    {
        tabItem.Status = "Data retrieved successfully"
    }

    if (statusUpdate.Status === "2002")
    {
        // we assume that the status contains error message regarding the last server
        var array_len = tabItem.office_dataArray.length
        var cur_servers_len = tabItem.office_dataArray[array_len-1].servers.length
        if (cur_servers_len > 0)
           tabItem.office_dataArray[array_len-1].servers[cur_servers_len-1].errorMsg = statusUpdate.statusText
        else
           console.log("We have received an error message outside the list of given servers: ", statusUpdate.statusText)
    }
    // federation information
    if (statusUpdate.Status === "3001") {
        clearInterval(tabItem.federationTimer)
    }

    if (statusUpdate.Status === "3000") {
        console.log("Federation: " + statusUpdate.Federation)
        console.log("ServerCount: " + statusUpdate.ServerCount)
        console.log("OperationalCount: " + statusUpdate.OperationalCount)

        tabItem.federations.push(statusUpdate)
    }
}

function fillGetStatusData() {
    return {
       messages: [],
       TestTime: "",
       IpInformation: {
          isAvailable: true,
          ATTModem: "",
          VerizonModem: ""
       },
       IpReachability : {
          isAvailable: true,
          ATTModemStatus: "",
          VerizonModemStatus: "",
          showConnectedVerizonConnected: false,
          showConnectedATTConnected: false
       },
       WifiInformation : {
          ClientId: "",
          AccessPoint: "",
          showConnected: false,
          showNotConnected: false,
          ClientStatus: ""
       },
       GatewayInformation: {
          ETMSClient: "",
          ETMSConnected: false,
          MDMClient: "",
          MDMConnected: false
       },
       RouteInformation: {
          ATTRoute: "",
          VerizonRoute: "",
          SprintRoute: "",
          WifiRoute: "",
          MHzRadio: "",
          ATTRouteTimestamp: "",
          VerizonRouteTimestamp: "",
          SprintRouteTimestamp: "",
          WifiRouteTimestamp: "",
          MHzRadioTimestamp: "",
          isATTStable: false,
          isVerizonStable: false,
          showRouteConnected: false,
          showRadioConnected: false,
          showSprintStable: false
       },
       RadioInformation: {
          RadioId: "",
          EMPAddress: ""
       },
       CellStatus: {
          status: "",
          showGood: false
       }
    }
}

function fillOfficeData() {
    new_office_data = []
    /*office_data1 = {}
    office_data1.header  = "Header1"
    office_data1.value  = "Value1"
    new_office_data.push(office_data1)

    office_data2 = {}
    office_data2.header  = "Header2"
    office_data2.value  = "Value2"
    new_office_data.push(office_data2) */

    return new_office_data
}

formatter = new Intl.DateTimeFormat('en', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
    hour12: true,
    timeZone: 'UTC'
})

function formatToUTC(millis) {
    if (millis > 0) {
        return formatter.format(new Date(millis))
    } else {
        return "Invalid"
    }
}
