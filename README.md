# Mazer

A game where you are in a maze and try to shot other people you find
in the maze.

Written with LibGDX, this game is intended for use on android devices and
desktops. The graphics are 3D first-person view, using a simple view to render the maze in front of the player. Geometric lines indicate the walls, floor and celine of the maze for the user. The user does have a map on the upper right and side that shows the maze but not other players. There is also a rear-view mirror for the user so they can see behind them. Players themselves are simple geometric shapes, like cubes, spheres or eyeballs. They can choose the shape when they begin if no other user picked the shape.

Users move forward by holding down the mouse or touching the screen if its a phone. By moving their pointer or finger to the left side of the screen they turn left, or right side they turn right. They can shoot in front of them by using the space bar or selecting a button on the screen. The bullet moves at a specific rate forward and stops either at a wall or another player. If a player gets hit they lose a point. All players start with 5 points, and lose when they get to zero. Last player alive wins. Note that when firing bullets, a bullet can only be fired once per second. Bullets move spaces in the maze every one second. 

When playing the game, one player acts as a host and the other players join them. The host player can show a QR code that the other players can scan to join. A player could also send a connect string via other means from the host game to get them to connect. 
