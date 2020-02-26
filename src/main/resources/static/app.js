var vue_det = new Vue({
    el: '#MainApp',
    data: {
        form: {
            LocoID: null,
            StartDate: null,
            EndDate: null,
            StartTime: null,
            EndTime: null,
            SCACMark: null
        },
        lstFullTree: [{
            value: null,
            text: 'Please select an option'
        }],
        show: true,
        bLogStatus: false,
        timerCheck: '',
        sStatus: '',
        sLogRequestStatus: '',
        timeUTC: '',
        userName: '',
        messageId: '',
        sFilesCount: 'Counting...',
        sTotalBytes: 'Checking log file...',
        showLinks: false,
    },
    created: function () {
        this.getUserName()
        this.fetchData()
        this.initDateTime()
        this.timeUTC = this.getUTCTime()
        this.timer = setInterval(this.getUTCTime, 5000)
    },
    methods: {
        onSubmit(evt) {
            evt.preventDefault()

            this.bLogStatus = true
            this.timer = setInterval(this.checkLogData, 1000)

            var xhr = new XMLHttpRequest()
            var self = this

            xhr.open('POST', 'request-logs')
            xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
            xhr.onload = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        self.sLogRequestStatus = "Request submitted successfully"
                        self.messageId = xhr.responseText
                    } else {
                        self.sLogRequestStatus = "Request status: ERROR, " + statusText
                        console.error('error - ' + xhr.statusText);
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
                            self.cancelLogDataCheck()
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

        }

    }
});