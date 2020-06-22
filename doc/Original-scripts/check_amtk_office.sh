#!/bin/sh
VERBOSE=$1
#if [ ! $VERBOSE = "-v" ]; then
#        VERBOSE=""
#fi
RED='\033[0;31m'
ORANGE='\033[0;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'
#KEY_FILE="/home/railwaynet/amtktestkeys/sample"
KEY_FILE="/home/railwaynet/.ssh/checkscripts"
REMOTE_SCRIPT="/home/railwaynet/check_aim.tcl $VERBOSE"
BOS_LIST=( 10.103.3.33 10.103.3.34 10.103.3.35 )
BOS_COUNT=1
BOS_ERROR_TEXT=""
clear
echo ""
echo ""
echo ""
echo -e "${BLUE}##############################################################################${NC}"
echo -e "${BLUE}#######               START COPY RIGHT HERE                            #######${NC}"
echo ""
echo -e "${MAGENTA}CIBOS System Status${NC}"
for i in "${BOS_LIST[@]}"
do
	BOS_OUTPUT=`ssh -i $KEY_FILE railwaynet@$i $REMOTE_SCRIPT`
	if [[ $BOS_OUTPUT = *"CRITICAL"* ]]; then
		echo -e "${RED}BOS $BOS_COUNT: \t\t\t\t [CRITICAL]${NC}"
		BOS_ERROR_TEXT="$BOS_ERROR_TEXT$BOS_OUTPUT \n"
	fi
	if [[ $BOS_OUTPUT = *"WARNING"* ]]; then
		echo -e "${ORANGE}BOS $BOS_COUNT: \t\t\t\t [WARNING]${NC}"
                BOS_ERROR_TEXT="$BOS_ERROR_TEXT$BOS_OUTPUT \n"
	fi
       if [[ $BOS_OUTPUT = *"OK"* ]]; then
                echo -e "${GREEN}BOS $BOS_COUNT: \t\t\t\t [OK]${NC}"
        fi
	let "BOS_COUNT++"
done

ARIS_LIST=( 10.103.3.39 10.103.3.40 10.103.3.41 )
ARIS_COUNT=1
echo -e "${MAGENTA}Nationwide ARIS System Status${NC}"
for i in "${ARIS_LIST[@]}"
do
        ARIS_OUTPUT=`ssh -i $KEY_FILE railwaynet@$i $REMOTE_SCRIPT`
        if [[ $ARIS_OUTPUT = *"CRITICAL"* ]]; then
                echo -e "${RED}ARIS $ARIS_COUNT: \t\t\t [CRITICAL]${NC}"
                BOS_ERROR_TEXT="$BOS_ERROR_TEXT$ARIS_OUTPUT \n"
        fi
        if [[ $ARIS_OUTPUT = *"WARNING"* ]]; then
                echo -e "${ORANGE}ARIS $ARIS_COUNT: \t\t\t [WARNING]${NC}"
                BOS_ERROR_TEXT="$BOS_ERROR_TEXT$ARIS_OUTPUT \n"
        fi
       if [[ $ARIS_OUTPUT = *"OK"* ]]; then
                echo -e "${GREEN}ARIS $ARIS_COUNT: \t\t\t [OK]${NC}"
        fi
        let "ARIS_COUNT++"
done

#NEC_GBOS_LIST=( 10.103.3.1 10.103.3.2 10.103.3.3 )
#NEC_GBOS_COUNT=1
#echo -e "${MAGENTA}NEC GBOS Status${NC}"
#for i in "${NEC_GBOS_LIST[@]}"
#do
#        ARIS_OUTPUT=`ssh -i $KEY_FILE railwaynet@$i $REMOTE_SCRIPT`
#        if [[ $NEC_GBOS_OUTPUT = *"CRITICAL"* ]]; then
#                echo -e "${RED}NEC GBOS $NEC_GBOS_COUNT: \t\t\t [CRITICAL]${NC}"
#                BOS_ERROR_TEXT="$BOS_ERROR_TEXT$NEC_GBOS_OUTPUT \n"
#        fi
#        if [[ $NEC_GBOS_OUTPUT = *"WARNING"* ]]; then
#                echo -e "${ORANGE}NEC GBOS $ARIS_COUNT: \t\t\t [WARNING]${NC}"
#                BOS_ERROR_TEXT="$BOS_ERROR_TEXT$NEC_GBOS_OUTPUT \n"
#        fi
#       if [[ $ARIS_OUTPUT = *"OK"* ]]; then
#                echo -e "${GREEN}NEC GBOS $NEC_GBOS_COUNT: \t\t\t [OK]${NC}"
#        fi
#        let "NEC_GBOS_COUNT++"
#done




MDM_LIST=( 10.103.3.36 10.103.3.37 10.103.3.38 )
MDM_COUNT=1
echo -e "${MAGENTA}Mobile Device Manager (MDM) System Status${NC}"
for i in "${MDM_LIST[@]}"
do
        MDM_OUTPUT=`ssh -i $KEY_FILE railwaynet@$i $REMOTE_SCRIPT`
        if [[ $MDM_OUTPUT = *"CRITICAL"* ]]; then
                echo -e "${RED}MDM $MDM_COUNT: \t\t\t\t [CRITICAL]${NC}"
                BOS_ERROR_TEXT="$BOS_ERROR_TEXT$MDM_OUTPUT \n"
        fi
        if [[ $MDM_OUTPUT = *"WARNING"* ]]; then
                echo -e "${ORANGE}MDM $MDM_COUNT: \t\t\t\t [WARNING]${NC}"
                BOS_ERROR_TEXT="$BOS_ERROR_TEXT$MDM_OUTPUT \n"
        fi
       if [[ $MDM_OUTPUT = *"OK"* ]]; then
                echo -e "${GREEN}MDM $MDM_COUNT: \t\t\t\t [OK]${NC}"
        fi
        let "MDM_COUNT++"
done
ARIS_KEY_FILE="/home/railwaynet/amtktestkeys/sample"
REMOTE_SCRIPT="bin/checkaws.sh"
ARIS_LIST=( 10.103.3.39 10.103.3.40 10.103.3.41 )
ARIS_COUNT=1
echo -e "${MAGENTA}Amazon Web Service MIS System Status${NC}"
for i in "${ARIS_LIST[@]}"
do
        AWS_OUTPUT=`ssh -q -i $ARIS_KEY_FILE railwaynet@$i $REMOTE_SCRIPT`
        if [[ $AWS_OUTPUT = *"Bad"* ]]; then
                echo -e "${RED}ARIS $ARIS_COUNT KeepAlive: \t\t [CRITICAL]${NC}"
        fi
       if [[ $AWS_OUTPUT = *"Good"* ]]; then
                echo -e "${GREEN}ARIS $ARIS_COUNT KeepAlive  \t\t [OK]${NC}"
        fi
        let "MDM_COUNT++"
        let "ARIS_COUNT++"
done
#Check SCAC ITCM
echo -e "${MAGENTA}Check Railroad ITCM Status"
echo -e "${CYAN}Broker Status \t\t\t [NOT IMPLEMENTED]"

#Check SCAC federation status with foreign railroad
if [[ $# -eq 1 ]]; then
echo -e "${MAGENTA}ITCM Federation Status${NC}"
	/usr/bin/python fed-check -i 10.102.6.100 -s $1
fi


echo ""
echo -e "${BLUE}ERROR TEXT${NC}"
echo -e ${CYAN}$BOS_ERROR_TEXT${NC}
echo -e ""
echo -e "${BLUE}#######                 END COPY RIGHT HERE                            #######${NC}"
echo -e "${BLUE}##############################################################################${NC}"
