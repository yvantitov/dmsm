# dmsm 1.12.2-1.0.0

Create dynmap markers with signs

## perms
To create or destroy markers of type `<marker set id>` a player requires:
`dmsm.<marker set id>`

e.g.

`dmsm.open_plot`

`dmsm.mini`

`dmsm.error`

## how to use
To create or destroy markers of type `<marker set id>` you must write `[<marker set id>]` in the top row of the sign, and nothing else. The other text on the sign will go into the marker's description.

## how to config
```
cool_house {
    keyword = "[coolhouse]"
    id = "cool_house"
    label = "Cool House"
    icon = "blueflag"
}
```

When added to the config, the above will instantiate a new marker set that players with permission `dmsm.cool_house` can create and destroy.

To create a sign marker of this type, the first line of the sign must be `[coolhouse]`.

On the dynmap, markers of this type will be labeled `Cool House` and have icon `blueflag`.
