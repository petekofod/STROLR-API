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
        bLogStatus: false,
        timerCheck: '',
        sStatus: '',
        sLogRequestStatus: '',
        timeUTC: '',
        userName: '',
        formTZ: '',
        messageId: '',
        cell_class: 'table-danger',
        isActive: false,
        tabName: '',
        active_tab: '',
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
            console.log(value)
            this.form.RequestType = value
        },
        onSubmit(evt) {
            console.log(this.form.RequestType)
            evt.preventDefault()

            var xhr = new XMLHttpRequest()
            var self = this

            xhr.open('POST', 'data-request')
            xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
            xhr.onload = async function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        self.new_messageId = xhr.responseText
                        new_tab_item = {}
                        new_tab_item.timer = setInterval(self.checkResponse.bind(null, self.new_messageId), 1000)

                        index = self.tab_data_Array.length
                        var title = "(" + index + ") " + self.form.SCACMark + " " + self.form.LocoID
                        if (self.form.RequestType == "get-status") {
                            title = title + " status"
                            new_status_data = self.fillGetStatusData()   // TODO this is test data just to show
                            self.tab_data_Array.push({id:index,
                                title: title,
                                LocoID: self.form.LocoID, status_data: new_status_data,
                                messageId: self.new_messageId, Status:"Loading locomotive system status...",
                                timer: new_tab_item.timer, sLogRequestStatus: "Request STATUS submitted successfully",
                                showFooter: false, isLogs: false, showLinks: false, isLogs:false})

                        } else if (self.form.RequestType == "get-logs") {
                            title = title + " logs"
                            self.tab_data_Array.push({id:index,
                                title: title,
                                LocoID: self.form.LocoID,
                                messageId: self.new_messageId, Status:"Loading locomotive logs...",
                                timer: new_tab_item.timer,
                                showFooter: true, isLogs: true, showLinks: false, isLogs:true,
                                sFilesCount: "Counting...", sTotalBytes: "Checking log file...", sLogRequestStatus: "Request STATUS submitted successfully"})
                        }
                        self.$nextTick(function () {
                            console.log("Title of the new tab is " + title)
                            self.active_tab = title
                        });
                    } else {
                                var errorMessage = JSON.parse(xhr.responseText).message
                                self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                                console.error('error - ' + errorMessage)
                                console.error(xhr.responseText)
                    }
                }
            }
            xhr.send(JSON.stringify(this.form))
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
                dataSCAC = JSON.parse(xhr.responseText)
                self.lstFullTree = self.lstFullTree.concat(dataSCAC.SCAC)
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
            console.log("checkResponse.... with ourMessageId", ourMessageId)
             var xhr = new XMLHttpRequest()
                    var self = this
                    xhr.open('GET', 'status-update/' + ourMessageId)
                    xhr.onload = function () {
                        console.log(" this is what is we have in response:", xhr.responseText)
                        if (xhr.readyState === 4) {
                            if (xhr.status === 200) {
                                statusUpdate = JSON.parse(xhr.responseText)
                                for (var i=0; i < self.tab_data_Array.length; i++) {
                                  if (self.tab_data_Array[i].messageId === ourMessageId) {
                                      console.log("Updating info according to response result")
                                      self.tab_data_Array[i].showFooter = false
                                      if (statusUpdate.statusText)
                                        self.tab_data_Array[i].status_data.Status = statusUpdate.statusText
                                      if (statusUpdate.TestTime)
                                        self.tab_data_Array[i].status_data.TestTime = statusUpdate.TestTime

                                      // ip information
                                      if (statusUpdate.VerizonModem)
                                         self.tab_data_Array[i].status_data.IpInformation.VerizonModem = statusUpdate.VerizonModem
                                      if (statusUpdate.ATTModem)
                                         self.tab_data_Array[i].status_data.IpInformation.ATTModem = statusUpdate.ATTModem

                                      // Ip reachability
                                      if (statusUpdate.ATTModemStatus) {
                                         self.tab_data_Array[i].status_data.IpReachability.ATTModemStatus = statusUpdate.ATTModemStatus
                                         if ( (statusUpdate.ATTModemStatus.toLowerCase()).includes("unreach") )
                                            self.tab_data_Array[i].status_data.IpReachability.showConnectedATTNotConnected = true
                                         else
                                            self.tab_data_Array[i].status_data.IpReachability.showConnectedATTConnected = true
                                      }
                                      if (statusUpdate.VerizonModemStatus) {
                                         self.tab_data_Array[i].status_data.IpReachability.VerizonModemStatus = statusUpdate.VerizonModemStatus
                                         if ( (statusUpdate.VerizonModemStatus.toLowerCase()).includes("online") )
                                            self.tab_data_Array[i].status_data.IpReachability.showConnectedVerizonConnected = true
                                         else
                                            self.tab_data_Array[i].status_data.IpReachability.showConnectedVerizonNotConnected = true
                                      }

                                      // Wifi information
                                      if (statusUpdate.WiFiClientStatus)
                                      {
                                         self.tab_data_Array[i].status_data.WifiInformation.ClientStatus = statusUpdate.WiFiClientStatus
                                         if ( (statusUpdate.WiFiClientStatus.toLowerCase()).includes("not connected") )
                                           self.tab_data_Array[i].status_data.WifiInformation.showNotConnected = true
                                         else
                                           self.tab_data_Array[i].status_data.WifiInformation.showConnected = true
                                      }
                                      if (statusUpdate.ClientId)
                                         self.tab_data_Array[i].status_data.WifiInformation.ClientId = statusUpdate.ClientId
                                      if (statusUpdate.AccessPoint)
                                         self.tab_data_Array[i].status_data.WifiInformation.AccessPoint = statusUpdate.AccessPoint

                                      // gateway information
                                      if (statusUpdate.ETMSClient) {
                                         self.tab_data_Array[i].status_data.GatewayInformation.ETMSClient = statusUpdate.ETMSClient
                                         if ( (statusUpdate.ETMSClient.toLowerCase()).includes("not connected") )
                                           self.tab_data_Array[i].status_data.GatewayInformation.showNotConnected = true
                                         else
                                           self.tab_data_Array[i].status_data.GatewayInformation.showConnected = true
                                      }
                                      if (statusUpdate.MDMClient)
                                      {
                                         self.tab_data_Array[i].status_data.GatewayInformation.MDMClient = statusUpdate.MDMClient
                                         if ((statusUpdate.MDMClient.toLowerCase()).includes("not connected"))
                                            self.tab_data_Array[i].status_data.GatewayInformation.showNotConnectedMSC = true
                                         else
                                            self.tab_data_Array[i].status_data.GatewayInformation.showConnectedMSC = true
                                      }

                                      // route information
                                      if (statusUpdate.ATTRoute) {
                                         self.tab_data_Array[i].status_data.RouteInformation.ATTRoute = statusUpdate.ATTRoute
                                         if ( (statusUpdate.ATTRoute.toLowerCase()).includes("not connected") )
                                            self.tab_data_Array[i].status_data.RouteInformation.showNotConnected = true
                                         else
                                            self.tab_data_Array[i].status_data.RouteInformation.showConnected = true
                                      }
                                      if (statusUpdate.VerizonRoute){
                                         self.tab_data_Array[i].status_data.RouteInformation.VerizonRoute = statusUpdate.VerizonRoute
                                         if ( (statusUpdate.VerizonRoute.toLowerCase()).includes("unstable") )
                                           self.tab_data_Array[i].status_data.RouteInformation.showNotStable = true
                                         else
                                           self.tab_data_Array[i].status_data.RouteInformation.showStable = true
                                      }
                                      if (statusUpdate.WifiRoute) {
                                         self.tab_data_Array[i].status_data.RouteInformation.WifiRoute = statusUpdate.WifiRoute
                                         if ((statusUpdate.WifiRoute.toLowerCase()).includes("not connected"))
                                            self.tab_data_Array[i].status_data.RouteInformation.showRouteNotConnected = true
                                         else
                                            self.tab_data_Array[i].status_data.RouteInformation.showRouteConnected = true
                                      }
                                      if (statusUpdate.MHzRadio) {
                                         self.tab_data_Array[i].status_data.RouteInformation.MHzRadio = statusUpdate.MHzRadio
                                         if ( !(statusUpdate.MHzRadio.toLowerCase()).includes("not connected") )
                                           self.tab_data_Array[i].status_data.RouteInformation.showRadioConnected = true
                                         else
                                           self.tab_data_Array[i].status_data.RouteInformation.showRadioNotConnected = true
                                      }

                                      // gateway information
                                      if (statusUpdate.RadioId)
                                         self.tab_data_Array[i].status_data.RadioInformation.RadioId = statusUpdate.RadioId
                                      if (statusUpdate.EMPAddress)
                                         self.tab_data_Array[i].status_data.RadioInformation.EMPAddress = statusUpdate.EMPAddress

                                      // logs information
                                      if (statusUpdate.filesCount)
                                         self.tab_data_Array[i].sFilesCount = statusUpdate.filesCount
                                      if (statusUpdate.totalBytes)
                                          self.tab_data_Array[i].sTotalBytes = statusUpdate.totalBytes
                                      if (statusUpdate.statusText)
                                         self.tab_data_Array[i].Status = statusUpdate.statusText
                                      console.log("going to  check status")
                                      if (statusUpdate.end) {
                                         console.log("stop the timer as the response has been processed")
                                         clearInterval(self.tab_data_Array[i].timer)
                                         if (self.tab_data_Array[i].isLogs)
                                            self.tab_data_Array[i].showLinks = true
                                      }
                                      // TODO process all other fields
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
                    }
                    xhr.send()
                },
         fillGetStatusData: function() {

           new_status_data = {}
           new_status_data.TestTime = ""
           ipinfo = {}
           ipinfo.isAvailable = true;
           ipinfo.ATTModem = ""
           ipinfo.VerizonModem = ""
           new_status_data.IpInformation = ipinfo

           ipreach = {}
           ipreach.isAvailable = true;
           ipreach.ATTModemStatus = ""
           ipreach.VerizonModemStatus = ""
           ipreach.showConnectedVerizonConnected = false
           ipreach.showConnectedVerizonNotConnected = false
           ipreach.showConnectedATTConnected = false
           ipreach.showConnectedATTNotConnected = false
           new_status_data.IpReachability = ipreach

           wifi_info = {}
           wifi_info.ClientId = ""
           wifi_info.AccessPoint = ""
           wifi_info.showConnected = false
           wifi_info.showNotConnected = false
           wifi_info.ClientStatus = ""
           new_status_data.WifiInformation = wifi_info

           gate_info = {}
           gate_info.ETMSClient= ""
           gate_info.showConnected = false
           gate_info.showNotConnected = false
           gate_info.showConnectedMSC = false
           gate_info.showNotConnectedMSC = false
           gate_info.MDMClient= ""
           new_status_data.GatewayInformation = gate_info

           route_info = {}
           route_info.ATTRoute=""
           route_info.VerizonRoute=""
           route_info.WifiRoute=""
           route_info.MHzRadio=""
           route_info.showConnected = false
           route_info.showNotConnected = false
           route_info.showStable= false
           route_info.showNotStable= false
           route_info.showRouteConnected = false
           route_info.showRouteNotConnected = false
           route_info.showRadioConnected = false
           route_info.showRadioNotConnected = false
           new_status_data.RouteInformation = route_info

           radio_info = {}
           radio_info.RadioId=""
           radio_info.EMPAddress=""

           new_status_data.RadioInformation = radio_info
           return new_status_data;

           },
    }
});