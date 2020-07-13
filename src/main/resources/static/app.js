Vue.config.devtools = true

var vue_det = new Vue({
    el: '#MainApp',
    data: {
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
        },
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
        tab_data_Array: [],
        show: true,
        timeUTC: '',
        userName: '',
        formTZ: '',
        cell_class: 'table-danger',
        isActive: false,
        active_tab: '',
        plugins: []
    },
    created: function () {
        this.getUserName()
        this.fetchData()
        this.initDateTime()
        this.timeUTC = this.getUTCTime()
        this.timer = setInterval(this.getUTCTime, 5000)
    },
    methods: {
        setRequestType(value) {
            this.form.RequestType = value
            if (this.form.RequestType != "get-backoffice")
            {
               LocoIdElement = document.getElementById("inpLocoID");
               LocoIdElement.setCustomValidity(this.validateLocoId()? "" : "LocoId should be set for this request");
            }
        },
        validateLocoId() {
           return this.form.LocoID != null
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
                        } else if (self.form.RequestType == "get-logs") {
                            var title = "(" + index + ") " + requestSCACMark + " " + requestLocoID + " logs"
                            var tabItem = createLogsTab(title, index, self.form, messageId, timer)
                        } else if (self.form.RequestType == "get-backoffice") {
                            var title = "(" + index + ") " + requestSCACMark + " office"
                            var tabItem = createOfficeTab(title, index, self.form, messageId, timer)
                            // Send additional request to get Federation status
                            // self.sendFederationRequest(requestSCACMark, messageId)
                        } else {
                            console.error("Unknown type of request!")
                            return
                        }
                        self.tab_data_Array.push(tabItem)

                        self.$nextTick(function () {
                            self.active_tab = title
                        });
                    } else {
                        var errorMessage = JSON.parse(xhr.responseText).message
                        self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                        console.error('error - ' + errorMessage)
                    }
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

            xhr.onload = async function () {
                if (xhr.readyState !== 4)
                    return
                if (xhr.status === 200) {
                    var messageId = xhr.responseText
                    var timer = setInterval(self.checkResponse.bind(null, messageId), 1000)
                    addFederationInfo(messageId, officeMessageId, timer)
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
            for (var i=0; i < self.tab_data_Array.length; i++) {
                if (self.tab_data_Array[i].messageIds.includes(officeMessageId)) {
                    self.tab_data_Array[i].messageIds.push(federationMessageId)
                    self.tab_data_Array[i].federationTimer = timer
                    break
                }
            }
        },
        onReset(evt) {
            evt.preventDefault()
            this.form.LocoID = null
            this.form.SCACMark = null
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
                if (conf.Plugins)
                    conf.Plugins.forEach(function (item) {
                      self.plugins.push(item)
                    })
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
        showFooter: false, isLogs: false, isStatus: true, isOffice: false, showLinks: false
    }
}

function createLogsTab(title, index, form, messageId, timer) {
    return {
        id: index,
        title: title,
        LocoID: form.LocoID,
        messageIds: [messageId],
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

        console.log("tabItem.office_dataArray WAS =", tabItem.office_dataArray)
        if (tabItem.office_dataArray.length > 0) {
           console.log("We are in the first part and length =", tabItem.office_dataArray.length)
           var array_len = tabItem.office_dataArray.length
           tabItem.office_dataArray[array_len-1].servers.push( {descr: statusUpdate.ServerType + " " + statusUpdate.ServerCount,
               value: statusUpdate.ServerStatus})
        }

        console.log("tabItem.office_dataArray BECAME =", tabItem.office_dataArray)
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

    // federation information
    if (statusUpdate.end === "2") {
        clearInterval(tabItem.federationTimer)
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
