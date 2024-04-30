wd=$(pwd)
cd $1
find . -name "*.java" > sources.txt
find . -name "*.class" -type f -delete
javac @sources.txt
cd "$wd"
