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
        tab_data : new Map(),
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
        sFilesCount: 'Counting...',
        sTotalBytes: 'Checking log file...',
        showLinks: false,
        showStatus:false,
        cell_class: 'table-danger',
        isActive: false,
        tabName: '',
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
            if (this.form.RequestType == "get-logs") {
                console.log("This is for GET LOGS request")

                this.bLogStatus = true
                this.timer = setInterval(this.checkLogData, 6000) //1000

                var xhr = new XMLHttpRequest()
                var self = this

                xhr.open('POST', 'data-request')
                xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
                xhr.onload = function () {
                    if (xhr.readyState === 4) {
                        if (xhr.status === 200) {
                            self.sLogRequestStatus = "Request submitted successfully"
                            self.messageId = xhr.responseText
                        } else {
                            var errorMessage = JSON.parse(xhr.responseText).message
                            self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                            console.error('error - ' + errorMessage)
                            console.error(xhr.responseText)
                        }
                    }
                }
                xhr.send(JSON.stringify(this.form))
            }
            if (this.form.RequestType == "get-status"){
               var xhr = new XMLHttpRequest()
               var self = this

                xhr.open('POST', 'data-request')
                xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
                xhr.onload = function () {
                if (xhr.readyState === 4) {
                      if (xhr.status === 200) {
                      self.sLogRequestStatus = "Request STATUS submitted successfully"
                      self.new_messageId = xhr.responseText
                      new_status_data = self.fillGetStatusData()
                      new_tab_item = {}
                      new_tab_item.status_data = new_status_data
                      new_tab_item.timer = setInterval(self.checkStatusData.bind(null, self.new_messageId), 6000)
                      self.tab_data.set(self.new_messageId, new_tab_item)
                      console.log(self.tab_data)
                      console.log(self.new_messageId)

                      index = self.tab_data_Array.length
                      self.tab_data_Array.push({id:index, title:"Locomotive System Status" + index, LocoID: self.form.LocoID, status_data: new_status_data,
                          messageId: self.new_messageId})

                      console.log("This is tab_data_array ", self.tab_data_Array)

                 } else {
                       var errorMessage = JSON.parse(xhr.responseText).message
                       self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                       console.error('error - ' + errorMessage)
                       console.error(xhr.responseText)
                   }
                 }
               }
                 xhr.send(JSON.stringify(this.form))
            } else {
               console.log("Unknown request. Do not know what to do with this yet")
            }
        },
        onReset(evt) {
            evt.preventDefault()
            this.form.LocoID = null
            this.form.SCACMark = null
            this.initDateTime()
            // Trick to reset/clear native browser form validation state
            this.show = false
            this.tab_data = []
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
        checkLogData: function () {
            var xhr = new XMLHttpRequest()
            var self = this

            xhr.open('GET', 'status-update/' + this.messageId)
            xhr.onload = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        statusUpdate = JSON.parse(xhr.responseText)
                        if (statusUpdate.filesCount)
                            self.sFilesCount = statusUpdate.filesCount
                        if (statusUpdate.totalBytes)
                            self.sTotalBytes = statusUpdate.totalBytes
                        if (statusUpdate.Status === "4") {
                            self.cancelLogDataCheck()   // TODO
                            self.showLinks = true
                        }
                        self.sStatus = statusUpdate.statusText
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
        cancelLogDataCheck() {
            clearInterval(this.timer)
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
            // var str = 'AM'
            var d = new Date();
            var hr = d.getUTCHours();
            var min = d.getUTCMinutes();
            var res;

            // if (hr == 0) {
            //     hr = 12
            // } else
            // if (hr > 11) {
            //     hr = hr - 11
            //     str = 'PM'
            // }
            if (hr < 10) hr = '0' + hr;
            if (min < 10) min = '0' + min;

            res = hr + ":" + min // + " " + str
            this.timeUTC = res
            return res

        },
        checkStatusData: function (ourMessageId) {
            console.log("checkStatusData.... with ourMessageId ", ourMessageId)
            console.log(this.tab_data)
             var xhr = new XMLHttpRequest()
                    var self = this
                    xhr.open('GET', 'status-update/' + ourMessageId)
                    xhr.onload = function () {
                        console.log(" this is what is we have in response:", xhr.responseText)
                        if (xhr.readyState === 4) {
                            if (xhr.status === 200) {
                                statusUpdate = JSON.parse(xhr.responseText)
                                if (statusUpdate.filesCount)
                                    self.sFilesCount = statusUpdate.filesCount
                                if (statusUpdate.totalBytes)
                                    self.sTotalBytes = statusUpdate.totalBytes
                                if (statusUpdate.Status === "1") {
                                    self.cancelLogDataCheck()  // TODO
                                    self.showLinks = true
                                }
                                self.sStatus = statusUpdate.statusText
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
           new_status_data.TestTime = "Tue Apr 21 11:14:02 EDT 2020"
           ipinfo = {}
           ipinfo.isAvailable = true;
           ipinfo.ATTModem = "10.149.1.42"
           ipinfo = VerizonModem = "10.148.0.33"
           new_status_data.IpInformation = ipinfo

           ipreach = {}
           ipreach.isAvailable = true;
           ipreach.ATTModemStatus = "Online"
           ipreach.VerizonModemStatus = "Unreachable"
           new_status_data.IpReachability = ipreach

           wifi_info = {}
           wifi_info.ClientId = "NTD4873"
           wifi_info.AccessPoint = "38:ED:18:E6:23"
           new_status_data.WifiInformation = wifi_info

           gate_info = {}
           gate_info.ETMSClient= "Connected"
           gate_info.MDMClient= "Connected"
           new_status_data.GatewayInformation = gate_info

           route_info = {}
           route_info.ATTRoute="Connected"
           route_info.VerizonRoute="Connected"
           route_info.WifiRoute="Not connected"
           route_info.MHzRadio="Connected"
           new_status_data.RouteInformation = route_info

           radio_info = {}
           radio_info.RadioId="10944515"
           radio_info.EMPAddress="netx. TNS_ELM+0.I. ant k.ant k+63. Radio o220"
           new_status_data.RadioInformation = radio_info
           return new_status_data;
           /*
           if (this.tab_data[index].status_data.IpReachability.VerizonModemStatus == "Unreachable")
               this.cell_class = 'table-danger'
           else
                this.cell_class = 'table-success'

           this.tabName = "Locomotive System Status" */

           },
    }
});