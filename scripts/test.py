"""AI-generated regression runner; uses synthetic fixtures and no school credentials."""
from pathlib import Path
import argparse,json,os,subprocess
def main():
 p=argparse.ArgumentParser();p.add_argument('--json-jar',required=True);a=p.parse_args();root=Path(__file__).resolve().parents[1]
 config=json.loads((root/'build/test-classpath.json').read_text(encoding='utf8'));out=root/'build/test-classes';out.mkdir(exist_ok=True);suffix='.exe' if os.name=='nt' else '';java=Path(config['java_bin'])
 cp=os.pathsep.join([config['classes'],str(Path(a.json_jar).resolve()),config['platform']]);subprocess.run([str(java/('javac'+suffix)),'-encoding','UTF-8','-cp',cp,'-d',str(out),*[str(x) for x in sorted((root/'tests').glob('*.java'))]],check=True)
 for test in ['AppPolicyTest','PageSwipeGestureTest','CourseModelTest','UpdatePolicyTest']:subprocess.run([str(java/('java'+suffix)),'-cp',str(out)+os.pathsep+cp,'cn.local.campuswater.'+test],check=True)
if __name__=='__main__':main()
