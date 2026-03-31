RED="\033[0;31m"
GREEN="\033[0;32m"
NC="\033[0m" # No Color

sh /work/compile.sh && echo "${GREEN}Downstream works${NC}" || echo "${RED}Downstream does not work${NC}" ;
sh /work/upstream.sh && echo "${GREEN}Upstream works${NC}" || echo "${RED}Upstream does not work${NC}"