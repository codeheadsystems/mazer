# Mazer

A retro-styled first-person maze shooter where you hunt down your friends in a neon-green wireframe labyrinth. Navigate tight corridors, check your rear-view mirror, and fire before they fire first. Last player standing wins.

## Gameplay

Drop into a procedurally generated maze rendered in glowing green wireframe — walls, floor, and ceiling all drawn with geometric lines on a black void. You see the world from a first-person perspective, with a minimap in the corner showing the maze layout (but never other players) and a rear-view mirror so nobody sneaks up behind you.

Players appear as wireframe shapes — cubes, spheres, or eyeballs — each in a distinct color. Pick your shape before the match starts. Up to 8 players can battle it out.

Every player starts with 5 hit points. Get shot, lose a point. Hit zero, you're out. The last player alive wins. Bullets travel at the same speed you walk, so dodging around corners is a real strategy. You can only fire once per second, so make your shots count.

## Controls

**Desktop:**
- Hold mouse button to move forward
- Move mouse left/right to steer
- Spacebar to fire

**Mobile:**
- Touch and hold to move forward
- Touch position controls steering (left side = turn left, right side = turn right)
- Tap the FIRE button in the lower-right corner

## Multiplayer

One player hosts, the others join. The host's lobby screen displays an IP address and QR code — scan it or type the IP to connect. Once everyone is in the lobby and ready, the host starts the match. Players spawn at random locations with a 5-second countdown, then it's on.

Currently supports LAN play. Internet play is planned for the future.

## Solo Mode

Don't have friends nearby? Solo mode drops you into a maze with stationary targets to hunt down. Great for learning the controls and getting a feel for the corridors.

## Tech

- Built with [LibGDX](https://libgdx.com/) in Java 17+
- Runs on Desktop (LWJGL3) and Android
- Networking via [KryoNet](https://github.com/crykn/kryonet) with host-authoritative simulation at 20Hz
- Procedural maze generation using recursive backtracker algorithm
- QR code generation via [ZXing](https://github.com/zxing/zxing)

## Building

```bash
# Run on desktop
gradle :desktop:run

# Build Android APK
gradle :android:assembleDebug
```

## Project Structure

```
core/    - Shared game code (rendering, world, networking, input)
desktop/ - Desktop launcher (LWJGL3)
android/ - Android launcher
```
