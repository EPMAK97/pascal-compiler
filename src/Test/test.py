from os import listdir
import re
import subprocess
import os

r = re.compile('(?P<name>.+)\.in')
for i in listdir('./'):
    name = r.match(i)
    if name:
        f1 = open('{}.out', 'w')
        subprocess.call(['java', '-jar', '/Users/ilyadonskoj/IdeaProjects/pascal_compiler/out/artifacts/pascal_compiler_jar/pascal_compiler.jar', '-l', i])
        f1, f2 = open('{}.out'.format(name.group('name'))), open('output.txt')
        a, b = f1.read(), f2.read();
        if a != b:
            print('Test "{}" failed'.format(i))
            print(b, file=open('{}.out'.format(name.group('name')), 'w'), end='')
        else:
            print('Test "{}" passed'.format(i))
        f1.close()
        f2.close()

os.remove('output.txt')