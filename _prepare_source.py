# -*- coding: utf-8 -*-
"""Prepare Clash Mate (CMFA) pinxixi fork."""
import os
import shutil
import subprocess

BASE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.join(BASE, "ClashMetaForAndroid")
PATCHES = os.path.join(BASE, "patches")


def run(cmd, cwd=None, t=900):
    print(f">>> {cmd}")
    r = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True, timeout=t)
    out = (r.stdout or "") + (r.stderr or "")
    print(out[-2000:] if len(out) > 2000 else out)
    if r.returncode != 0:
        raise RuntimeError(f"failed: {cmd}")


def main():
    if not os.path.isdir(REPO):
        run(
            "git clone --depth 1 https://github.com/MetaCubeX/ClashMetaForAndroid.git ClashMetaForAndroid",
            cwd=BASE,
        )

    copies = [
        ("common/Pinxixi.kt", "common/src/main/java/com/github/kr328/clash/common/pinxixi/Pinxixi.kt"),
        ("service/ProfileManager.kt", "service/src/main/java/com/github/kr328/clash/service/ProfileManager.kt"),
        ("service/ProfileProcessor.kt", "service/src/main/java/com/github/kr328/clash/service/ProfileProcessor.kt"),
        ("service/ProfileReceiver.kt", "service/src/main/java/com/github/kr328/clash/service/ProfileReceiver.kt"),
        ("app/ExternalControlActivity.kt", "app/src/main/java/com/github/kr328/clash/ExternalControlActivity.kt"),
        ("design/strings.xml", "design/src/main/res/values/strings.xml"),
        ("build.gradle.kts", "build.gradle.kts"),
    ]
    for rel_src, rel_dst in copies:
        src = os.path.join(PATCHES, rel_src)
        dst = os.path.join(REPO, rel_dst)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(src, dst)
        print(f"copied {rel_dst}")

    props = os.path.join(REPO, "local.properties")
    if not os.path.isfile(props):
        with open(props, "w", encoding="utf-8") as f:
            f.write("custom.application.id=com.wzpxx.clashmate.dev\n")
            f.write("remove.suffix=true\n")
        print("wrote local.properties (app id only; CI adds sdk.dir)")

    print("SOURCE READY at", REPO)


if __name__ == "__main__":
    main()
