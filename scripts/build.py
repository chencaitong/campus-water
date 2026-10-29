"""AI-generated portable Android build; no signing key or password is included."""
from pathlib import Path
import argparse,json,os,shutil,subprocess,tempfile,zipfile

def run(args):subprocess.run([str(x) for x in args],check=True)
def main():
 p=argparse.ArgumentParser();p.add_argument('--sdk',default=os.getenv('ANDROID_SDK_ROOT') or os.getenv('ANDROID_HOME'));p.add_argument('--java-home',default=os.getenv('JAVA_HOME'));p.add_argument('--build-tools',default='35.0.0');p.add_argument('--platform',default='35');p.add_argument('--tools-dir');p.add_argument('--android-jar');p.add_argument('--keystore');p.add_argument('--alias',default='release');p.add_argument('--store-pass-env',default='KEYSTORE_PASSWORD');p.add_argument('--key-pass-env');a=p.parse_args()
 if not a.sdk:p.error('Set ANDROID_SDK_ROOT or provide --sdk')
 root=Path(__file__).resolve().parents[1];sdk=Path(a.sdk).resolve();tools=Path(a.tools_dir).resolve() if a.tools_dir else sdk/'build-tools'/a.build_tools;platform=Path(a.android_jar).resolve() if a.android_jar else sdk/'platforms'/('android-'+a.platform)/'android.jar';suffix='.exe' if os.name=='nt' else ''
 java_bin=Path(a.java_home)/'bin' if a.java_home else Path(shutil.which('javac') or '').resolve().parent
 javac=java_bin/('javac'+suffix);java=java_bin/('java'+suffix)
 for path in [javac,java,platform,tools/('aapt2'+suffix),tools/('zipalign'+suffix),tools/'lib/d8.jar']:
  if not path.is_file():p.error('Required build input not found: '+str(path))
 build=root/'build';build.mkdir(exist_ok=True);classes=Path(tempfile.mkdtemp(prefix='classes-',dir=build));dex=Path(tempfile.mkdtemp(prefix='dex-',dir=build));resources=build/'resources.zip';base=build/'base.apk'
 if resources.exists():resources.unlink()
 run([tools/('aapt2'+suffix),'compile','--dir',root/'res','-o',resources]);run([tools/('aapt2'+suffix),'link','-o',base,'--manifest',root/'AndroidManifest.xml','-I',platform,resources])
 boot=os.pathsep.join([str(platform),str(tools/'core-lambda-stubs.jar')]);run([javac,'-encoding','UTF-8','-source','8','-target','8','-bootclasspath',boot,'-d',classes,*sorted((root/'src').rglob('*.java'))])
 with zipfile.ZipFile(build/'classes.jar','w') as z:
  for file in sorted(classes.rglob('*.class')):z.write(file,file.relative_to(classes))
 run([java,'-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--lib',platform,'--min-api','26','--output',dex,build/'classes.jar'])
 with zipfile.ZipFile(base,'a') as z:z.write(dex/'classes.dex','classes.dex')
 unsigned=build/'campus-water-1.0-unsigned.apk';run([tools/('zipalign'+suffix),'-f','4',base,unsigned]);output=unsigned
 if a.keystore:
  if not os.getenv(a.store_pass_env):p.error('Signing password environment variable is not set')
  output=build/'campus-water-1.0.apk';command=[java,'-jar',tools/'lib/apksigner.jar','sign','--ks',Path(a.keystore).resolve(),'--ks-key-alias',a.alias,'--ks-pass','env:'+a.store_pass_env,'--out',output,unsigned]
  if a.key_pass_env:command.extend(['--key-pass','env:'+a.key_pass_env])
  run(command);run([java,'-jar',tools/'lib/apksigner.jar','verify',output])
 (build/'test-classpath.json').write_text(json.dumps({'classes':str(classes),'platform':str(platform),'java_bin':str(java_bin)}),encoding='utf8')
 print('Built:',output)
if __name__=='__main__':main()
